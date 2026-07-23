// PortalHost Website - Main JavaScript

// GitHub API Configuration
const GITHUB_REPO = 'dhyllrocoedu-ai/portalhosting';
const GITHUB_API_BASE = `https://api.github.com/repos/${GITHUB_REPO}`;

// Utility Functions
function showLoading(element) {
    element.innerHTML = `
        <div class="loading">
            <div class="spinner"></div>
        </div>
    `;
}

function showError(element, message = 'Failed to load data') {
    element.innerHTML = `
        <div class="error-message">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
            <p>${message}</p>
            <button class="btn btn-secondary" onclick="location.reload()">Retry</button>
        </div>
    `;
}

async function fetchGitHub(endpoint) {
    const response = await fetch(`${GITHUB_API_BASE}${endpoint}`, {
        headers: {
            'Accept': 'application/vnd.github.v3+json',
            'User-Agent': 'PortalHost-Website'
        }
    });
    if (!response.ok) {
        throw new Error(`GitHub API error: ${response.status}`);
    }
    return response.json();
}

// Navbar Scroll Behavior
let lastScrollY = 0;
const navbar = document.querySelector('.navbar');

function handleNavbarScroll() {
    const currentScrollY = window.scrollY;
    
    if (currentScrollY > 100) {
        navbar.classList.add('scrolled');
    } else {
        navbar.classList.remove('scrolled');
    }
    
    if (currentScrollY > lastScrollY && currentScrollY > 100) {
        navbar.classList.add('hidden');
    } else {
        navbar.classList.remove('hidden');
    }
    
    lastScrollY = currentScrollY;
}

// Mobile Menu Toggle
function initMobileMenu() {
    const menuBtn = document.querySelector('.mobile-menu-btn');
    const navLinks = document.querySelector('.nav-links');
    
    if (menuBtn && navLinks) {
        menuBtn.addEventListener('click', () => {
            navLinks.classList.toggle('open');
        });
    }
    
    // Close menu when clicking a link
    document.querySelectorAll('.nav-links a').forEach(link => {
        link.addEventListener('click', () => {
            navLinks.classList.remove('open');
        });
    });
}

// Set Active Nav Link
function setActiveNavLink() {
    const currentPath = window.location.pathname.split('/').pop() || 'index.html';
    document.querySelectorAll('.nav-links a').forEach(link => {
        const href = link.getAttribute('href');
        if (href === currentPath || (currentPath === '' && href === 'index.html')) {
            link.classList.add('active');
        } else {
            link.classList.remove('active');
        }
    });
}

// Particle Animation
function initParticles() {
    const canvas = document.createElement('canvas');
    canvas.className = 'particles-canvas';
    document.body.prepend(canvas);
    
    const ctx = canvas.getContext('2d');
    let particles = [];
    let animationId;
    
    function resize() {
        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight;
    }
    
    function createParticle() {
        return {
            x: Math.random() * canvas.width,
            y: Math.random() * canvas.height,
            vx: (Math.random() - 0.5) * 0.5,
            vy: (Math.random() - 0.5) * 0.5,
            radius: Math.random() * 2 + 1,
            opacity: Math.random() * 0.5 + 0.1,
            color: Math.random() > 0.5 ? '#00d4aa' : '#00ff88'
        };
    }
    
    function init() {
        particles = [];
        const count = Math.min(80, Math.floor((canvas.width * canvas.height) / 15000));
        for (let i = 0; i < count; i++) {
            particles.push(createParticle());
        }
    }
    
    function animate() {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        
        particles.forEach(p => {
            p.x += p.vx;
            p.y += p.vy;
            
            if (p.x < 0) p.x = canvas.width;
            if (p.x > canvas.width) p.x = 0;
            if (p.y < 0) p.y = canvas.height;
            if (p.y > canvas.height) p.y = 0;
            
            ctx.beginPath();
            ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
            ctx.fillStyle = p.color;
            ctx.globalAlpha = p.opacity;
            ctx.fill();
            ctx.globalAlpha = 1;
        });
        
        // Draw connections
        for (let i = 0; i < particles.length; i++) {
            for (let j = i + 1; j < particles.length; j++) {
                const dx = particles[i].x - particles[j].x;
                const dy = particles[i].y - particles[j].y;
                const dist = Math.sqrt(dx * dx + dy * dy);
                
                if (dist < 120) {
                    ctx.beginPath();
                    ctx.moveTo(particles[i].x, particles[i].y);
                    ctx.lineTo(particles[j].x, particles[j].y);
                    ctx.strokeStyle = particles[i].color;
                    ctx.globalAlpha = (1 - dist / 120) * 0.15;
                    ctx.lineWidth = 1;
                    ctx.stroke();
                    ctx.globalAlpha = 1;
                }
            }
        }
        
        animationId = requestAnimationFrame(animate);
    }
    
    window.addEventListener('resize', () => {
        resize();
        init();
    });
    
    resize();
    init();
    animate();
    
    // Cleanup on page unload
    window.addEventListener('beforeunload', () => {
        cancelAnimationFrame(animationId);
    });
}

// Format Date
function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
    });
}

// Format File Size
function formatBytes(bytes) {
    if (!bytes) return 'Unknown size';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
}

// Initialize on DOM Ready
document.addEventListener('DOMContentLoaded', () => {
    handleNavbarScroll();
    window.addEventListener('scroll', handleNavbarScroll, { passive: true });
    initMobileMenu();
    setActiveNavLink();
    initParticles();
});

// Export for use in page-specific scripts
window.PortalHost = {
    GITHUB_REPO,
    GITHUB_API_BASE,
    fetchGitHub,
    showLoading,
    showError,
    formatDate,
    formatBytes
};