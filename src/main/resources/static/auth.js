// auth.js
document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('signupForm');
    const successMessage = document.getElementById('successMessage');

    // Function to validate email format
    function isValidEmail(email) {
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
    }

    // Function to validate password strength
    function isValidPassword(password) {
        return password.length >= 8 && 
               /[A-Z]/.test(password) && 
               /[a-z]/.test(password) && 
               /[0-9]/.test(password);
    }

    // Function to show error message
    function showError(elementId, message) {
        const errorElement = document.getElementById(elementId);
        errorElement.textContent = message;
        errorElement.style.display = 'block';
    }

    // Function to hide error message
    function hideError(elementId) {
        const errorElement = document.getElementById(elementId);
        errorElement.style.display = 'none';
    }

    // Form submission handler
    form.addEventListener('submit', function(e) {
        e.preventDefault();
        
        const username = document.getElementById('username').value;
        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;
        const confirmPassword = document.getElementById('confirmPassword').value;
        
        let isValid = true;

        // Reset all error messages
        hideError('usernameError');
        hideError('emailError');
        hideError('passwordError');
        hideError('confirmPasswordError');
        successMessage.style.display = 'none';

        // Validate username
        if (username.length < 3) {
            showError('usernameError', 'Username must be at least 3 characters long');
            isValid = false;
        }

        // Validate email
        if (!isValidEmail(email)) {
            showError('emailError', 'Please enter a valid email address');
            isValid = false;
        }

        // Validate password
        if (!isValidPassword(password)) {
            showError('passwordError', 'Password must be at least 8 characters long and contain uppercase, lowercase, and numbers');
            isValid = false;
        }

        // Validate password confirmation
        if (password !== confirmPassword) {
            showError('confirmPasswordError', 'Passwords do not match');
            isValid = false;
        }

        if (isValid) {
            // Store user data (in real app, this would be handled by backend)
            const userData = {
                username: username,
                email: email
            };
            localStorage.setItem('userData', JSON.stringify(userData));

            // Show success message briefly
            successMessage.style.display = 'block';
            
            // Redirect to dashboard after short delay
            setTimeout(() => {
                window.location.href = 'dashboard.html';
            }, 1500);

            /* In a real application, you would send to backend:
            fetch('/api/signup', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    username,
                    email,
                    password
                })
            })
            .then(response => response.json())
            .then(data => {
                localStorage.setItem('userData', JSON.stringify(data.user));
                localStorage.setItem('token', data.token);
                window.location.href = 'dashboard.html';
            })
            .catch(error => {
                showError('generalError', 'An error occurred. Please try again.');
            });
            */
        }
    });
});