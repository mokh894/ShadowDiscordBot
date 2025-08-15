// Dashboard JavaScript functionality

// Utility functions
function showMessage(message, type = 'info') {
    const messageContainer = document.getElementById('message-container');
    if (!messageContainer) {
        console.warn('Message container not found');
        return;
    }
    
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type} alert-dismissible fade show`;
    alertDiv.innerHTML = `
        ${message}
        <button type="button" class="btn-close" onclick="this.parentElement.remove()"></button>
    `;
    
    messageContainer.appendChild(alertDiv);
    
    // Auto-remove after 5 seconds
    setTimeout(() => {
        if (alertDiv.parentNode) {
            alertDiv.remove();
        }
    }, 5000);
}

// Form submission
function submitForm(formId, url, successMessage = 'Configuration saved successfully!') {
    const form = document.getElementById(formId);
    if (!form) {
        console.error('Form not found:', formId);
        return;
    }
    
    const formData = new FormData(form);
    const data = {};
    
    // Convert form data to object
    for (let [key, value] of formData.entries()) {
        if (key.includes('[') && key.includes(']')) {
            // Handle nested objects like enabledCommands[help]
            const parts = key.split(/\[|\]/).filter(p => p);
            if (parts.length === 2) {
                if (!data[parts[0]]) data[parts[0]] = {};
                data[parts[0]][parts[1]] = value === 'on' ? true : value;
            }
        } else {
            data[key] = value === 'on' ? true : value;
        }
    }
    
    // Handle checkboxes (unchecked boxes don't appear in FormData)
    const checkboxes = form.querySelectorAll('input[type="checkbox"]');
    checkboxes.forEach(checkbox => {
        if (checkbox.name.includes('[') && checkbox.name.includes(']')) {
            const parts = checkbox.name.split(/\[|\]/).filter(p => p);
            if (parts.length === 2) {
                if (!data[parts[0]]) data[parts[0]] = {};
                if (!data[parts[0]][parts[1]]) {
                    data[parts[0]][parts[1]] = false;
                }
            }
        } else if (!data[checkbox.name]) {
            data[checkbox.name] = false;
        }
    });
    
    // Show loading state
    const submitButton = form.querySelector('button[type="submit"], button[onclick*="submitForm"]');
    if (submitButton) {
        const originalText = submitButton.innerHTML;
        submitButton.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Saving...';
        submitButton.disabled = true;

        fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data)
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showMessage(data.message || successMessage, 'success');
            } else {
                showMessage(data.message || 'An error occurred while saving.', 'danger');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showMessage('An error occurred while saving the configuration.', 'danger');
        })
        .finally(() => {
            submitButton.innerHTML = originalText;
            submitButton.disabled = false;
        });
    }
}

// Command toggle functionality
function toggleCommand(commandName, checkbox) {
    const isEnabled = checkbox.checked;
    const commandRow = checkbox.closest('.command-toggle');
    
    if (commandRow) {
        if (isEnabled) {
            commandRow.classList.add('border-success');
            commandRow.classList.remove('border-danger');
        } else {
            commandRow.classList.add('border-danger');
            commandRow.classList.remove('border-success');
        }
    }
}

// Modal functionality
function showModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.style.display = 'block';
        modal.classList.add('show');
        document.body.classList.add('modal-open');
    }
}

function hideModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.style.display = 'none';
        modal.classList.remove('show');
        document.body.classList.remove('modal-open');
    }
}

// Close modal when clicking outside
document.addEventListener('DOMContentLoaded', function() {
    // Close modals when clicking outside
    const modals = document.querySelectorAll('.modal');
    modals.forEach(modal => {
        modal.addEventListener('click', function(e) {
            if (e.target === modal) {
                hideModal(modal.id);
            }
        });
    });
    
    // Close modals with escape key
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            const openModals = document.querySelectorAll('.modal.show');
            openModals.forEach(modal => {
                hideModal(modal.id);
            });
        }
    });
    
    // Initialize tooltips if needed
    const tooltips = document.querySelectorAll('[data-bs-toggle="tooltip"]');
    tooltips.forEach(tooltip => {
        tooltip.title = tooltip.getAttribute('data-bs-title') || tooltip.title;
    });
});

// Utility function to format numbers
function formatNumber(num) {
    if (num >= 1000000) {
        return (num / 1000000).toFixed(1) + 'M';
    } else if (num >= 1000) {
        return (num / 1000).toFixed(1) + 'K';
    }
    return num.toString();
}

// Utility function to format dates
function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
}

// Copy to clipboard functionality
function copyToClipboard(text) {
    navigator.clipboard.writeText(text).then(() => {
        showMessage('Copied to clipboard!', 'success');
    }).catch(err => {
        console.error('Failed to copy: ', err);
        showMessage('Failed to copy to clipboard', 'danger');
    });
}
