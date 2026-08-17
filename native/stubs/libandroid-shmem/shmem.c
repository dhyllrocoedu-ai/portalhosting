/*
 * Minimal memfd-backed emulation of System V shared memory for the JVM.
 *
 * Termux's openjdk-21 build of libjvm.so has DT_NEEDED libandroid-shmem.so
 * and imports libandroid_shmget / libandroid_shmat / libandroid_shmctl /
 * libandroid_shmdt.  Bionic does not provide these, so we provide a small
 * self-contained implementation backed by memfd (kernel >= 3.17).
 *
 * Exported symbols match the real libandroid-shmem 0.7 (see exports.txt).
 */
#define _GNU_SOURCE

#include <errno.h>
#include <pthread.h>
#include <stddef.h>
#include <stdint.h>
#include <string.h>
#include <sys/ipc.h>
#include <sys/mman.h>
#include <sys/shm.h>
#include <sys/syscall.h>
#include <sys/types.h>
#include <unistd.h>

#ifndef __NR_memfd_create
#if defined(__aarch64__)
#define __NR_memfd_create 279
#elif defined(__x86_64__)
#define __NR_memfd_create 319
#else
#define __NR_memfd_create 279
#endif
#endif

#define MAX_SEG 64
#define ROUND_UP(n, s) (((n) + (s) - 1) / (s) * (s))

typedef struct {
    int shmid;
    key_t key;
    int fd;
    size_t size;
    void *addr;
    int nattch;
    int mark_delete;
    int used;
} seg_t;

static seg_t g_segs[MAX_SEG];
static int g_next_shmid = 0;
static pthread_mutex_t g_lock = PTHREAD_MUTEX_INITIALIZER;

static int find_by_shmid(int shmid)
{
    for (int i = 0; i < MAX_SEG; i++)
        if (g_segs[i].used && g_segs[i].shmid == shmid) return i;
    return -1;
}

static int find_by_key(key_t key, size_t size)
{
    for (int i = 0; i < MAX_SEG; i++)
        if (g_segs[i].used && g_segs[i].key == key && g_segs[i].size >= size)
            return i;
    return -1;
}

static int find_by_addr(const void *addr)
{
    for (int i = 0; i < MAX_SEG; i++)
        if (g_segs[i].used && g_segs[i].addr == addr) return i;
    return -1;
}

static void free_seg(int i)
{
    if (g_segs[i].fd >= 0) close(g_segs[i].fd);
    memset(&g_segs[i], 0, sizeof(seg_t));
    g_segs[i].fd = -1;
}

static int alloc_shmid(void)
{
    if (g_next_shmid < 0 || g_next_shmid == INT32_MAX) g_next_shmid = 0;
    return ++g_next_shmid;
}

int libandroid_shmget(key_t key, size_t size, int shmflg)
{
    (void)shmflg;

    long pgsz = sysconf(_SC_PAGESIZE);
    if (pgsz <= 0) pgsz = 4096;
    size_t sz = ROUND_UP(size, (size_t)pgsz);
    if (sz == 0) sz = (size_t)pgsz;

    pthread_mutex_lock(&g_lock);

    if (key != IPC_PRIVATE) {
        int i = find_by_key(key, sz);
        if (i != -1) {
            pthread_mutex_unlock(&g_lock);
            return g_segs[i].shmid;
        }
    }

    int fd = (int)syscall(__NR_memfd_create, "jvm-shm", 0);
    if (fd < 0) {
        pthread_mutex_unlock(&g_lock);
        return -1;
    }
    if (ftruncate(fd, (off_t)sz) != 0) {
        close(fd);
        pthread_mutex_unlock(&g_lock);
        return -1;
    }

    int idx = -1;
    for (int i = 0; i < MAX_SEG; i++) {
        if (!g_segs[i].used) { idx = i; break; }
    }
    if (idx == -1) {
        close(fd);
        pthread_mutex_unlock(&g_lock);
        errno = ENOSPC;
        return -1;
    }

    int shmid = alloc_shmid();
    g_segs[idx].shmid = shmid;
    g_segs[idx].key = key;
    g_segs[idx].fd = fd;
    g_segs[idx].size = sz;
    g_segs[idx].addr = NULL;
    g_segs[idx].nattch = 0;
    g_segs[idx].mark_delete = 0;
    g_segs[idx].used = 1;

    pthread_mutex_unlock(&g_lock);
    return shmid;
}

void *libandroid_shmat(int shmid, const void *shmaddr, int shmflg)
{
    pthread_mutex_lock(&g_lock);
    int i = find_by_shmid(shmid);
    if (i == -1) {
        pthread_mutex_unlock(&g_lock);
        errno = EINVAL;
        return (void *)-1;
    }
    if (g_segs[i].addr != NULL) {
        void *a = g_segs[i].addr;
        pthread_mutex_unlock(&g_lock);
        return a;
    }

    int prot = PROT_READ;
    if ((shmflg & SHM_RDONLY) == 0) prot |= PROT_WRITE;
    int flags = MAP_SHARED;
    if (shmaddr != NULL) flags |= MAP_FIXED;

    void *addr = mmap((void *)shmaddr, g_segs[i].size, prot, flags,
                      g_segs[i].fd, 0);
    if (addr == MAP_FAILED) {
        pthread_mutex_unlock(&g_lock);
        return (void *)-1;
    }
    g_segs[i].addr = addr;
    g_segs[i].nattch++;
    pthread_mutex_unlock(&g_lock);
    return addr;
}

int libandroid_shmdt(const void *shmaddr)
{
    pthread_mutex_lock(&g_lock);
    int i = find_by_addr(shmaddr);
    if (i == -1) {
        pthread_mutex_unlock(&g_lock);
        return 0;
    }
    if (g_segs[i].addr != NULL) {
        munmap(g_segs[i].addr, g_segs[i].size);
        g_segs[i].addr = NULL;
        g_segs[i].nattch--;
    }
    if (g_segs[i].mark_delete && g_segs[i].nattch <= 0) {
        free_seg(i);
    }
    pthread_mutex_unlock(&g_lock);
    return 0;
}

int libandroid_shmctl(int shmid, int cmd, struct shmid_ds *buf)
{
    pthread_mutex_lock(&g_lock);
    int i = find_by_shmid(shmid);
    if (i == -1) {
        pthread_mutex_unlock(&g_lock);
        errno = EINVAL;
        return -1;
    }

    if (cmd == IPC_RMID) {
        if (g_segs[i].addr != NULL) {
            g_segs[i].mark_delete = 1;
        } else {
            free_seg(i);
        }
        pthread_mutex_unlock(&g_lock);
        return 0;
    }

    if (cmd == IPC_STAT) {
        if (!buf) {
            pthread_mutex_unlock(&g_lock);
            errno = EINVAL;
            return -1;
        }
        memset(buf, 0, sizeof(*buf));
        buf->shm_segsz = g_segs[i].size;
        buf->shm_nattch = (unsigned short)(g_segs[i].nattch > 0 ? g_segs[i].nattch : 1);
        buf->shm_perm.key = g_segs[i].key;
        buf->shm_perm.uid = (unsigned short)getuid();
        buf->shm_perm.gid = (unsigned short)getgid();
        buf->shm_perm.cuid = (unsigned short)getuid();
        buf->shm_perm.cgid = (unsigned short)getgid();
        buf->shm_perm.mode = 0666;
        buf->shm_perm.seq = 1;
        pthread_mutex_unlock(&g_lock);
        return 0;
    }

    if (cmd == IPC_SET) {
        pthread_mutex_unlock(&g_lock);
        return 0;
    }

    pthread_mutex_unlock(&g_lock);
    errno = EINVAL;
    return -1;
}

/* PRoot helpers exported by the real library. */
int libandroid_shmat_fd(int shmid, size_t *out_size)
{
    pthread_mutex_lock(&g_lock);
    int i = find_by_shmid(shmid);
    if (i == -1) {
        pthread_mutex_unlock(&g_lock);
        errno = EINVAL;
        return -1;
    }
    if (out_size) *out_size = g_segs[i].size;
    int fd = g_segs[i].fd;
    pthread_mutex_unlock(&g_lock);
    return fd;
}

int libandroid_shmdt_fd(int fd)
{
    pthread_mutex_lock(&g_lock);
    for (int i = 0; i < MAX_SEG; i++) {
        if (g_segs[i].used && g_segs[i].fd == fd) {
            free_seg(i);
            pthread_mutex_unlock(&g_lock);
            return 0;
        }
    }
    pthread_mutex_unlock(&g_lock);
    return 0;
}

#undef shmget
int shmget(key_t key, size_t size, int shmflg) __attribute__((alias("libandroid_shmget")));
#undef shmat
void *shmat(int shmid, const void *shmaddr, int shmflg) __attribute__((alias("libandroid_shmat")));
#undef shmdt
int shmdt(const void *shmaddr) __attribute__((alias("libandroid_shmdt")));
#undef shmctl
int shmctl(int shmid, int cmd, struct shmid_ds *buf) __attribute__((alias("libandroid_shmctl")));
