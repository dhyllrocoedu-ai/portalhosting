/*
 * Placeholder for Termux's libandroid-spawn.so.
 *
 * libjvm.so / libjava.so list libandroid-spawn.so as DT_NEEDED and import
 * posix_spawn, which is already provided by bionic libc.  This library only
 * needs to exist and load cleanly so the dynamic linker is satisfied.
 */
int libandroid_spawn_dummy(void)
{
    return 0;
}
