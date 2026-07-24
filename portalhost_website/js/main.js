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