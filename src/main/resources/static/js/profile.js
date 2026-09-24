let currentUser = null;
let currentOrders = [];

document.addEventListener("DOMContentLoaded", async function () {
    applyTranslations();

    document.addEventListener("languageChanged", () => {
        applyTranslations();
        renderUserInfo();
        renderThemeSelection();

        if (currentOrders.length > 0) {
            renderOrders(currentOrders);
        }
    });

    try {
        const response = await fetch("http://localhost:8080/auth/me", {
            credentials: "include"
        });

        if (!response.ok) {
            throw new Error("Not authenticated");
        }

        currentUser = await response.json();

        renderUserInfo();
        await loadOrders();

    } catch (error) {
        console.error(error);
        alert(t("profile.messages.notLoggedIn"));
        window.location.href = "/html/signIn.html";
    }
});

function renderUserInfo() {
    if (!currentUser) return;

    document.getElementById("emailDisplay").textContent = currentUser.email;
    document.getElementById("usernameDisplay").textContent = currentUser.username;
    document.getElementById("roleDisplay").textContent = currentUser.role;
}

// ======================
// PROFILE MODAL
// ======================

function openModal() {
    document.getElementById("email").value = currentUser.email;
    document.getElementById("username").value = currentUser.username;
    document.getElementById("updateProfileModal").style.display = "block";
}

function closeModal() {
    document.getElementById("updateProfileModal").style.display = "none";
}

// ======================
// UPDATE PROFILE
// ======================

async function updateProfile() {
    const updatedUser = {
        email: document.getElementById("email").value.trim(),
        username: document.getElementById("username").value.trim()
    };

    try {
        const response = await fetch(`http://localhost:8080/users/${currentUser.id}`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            credentials: "include",
            body: JSON.stringify(updatedUser)
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || "Update failed");
        }

        currentUser = await response.json();

        renderUserInfo();

        alert(t("profile.messages.profileUpdated"));
        closeModal();

    } catch (error) {
        console.error(error);
        alert(t("profile.messages.profileUpdateError"));
    }
}

// ======================
// UPDATE PASSWORD
// ======================

async function updatePassword() {
    const newPassword = document.getElementById("newPassword").value.trim();
    const confirmPassword = document.getElementById("confirmPassword").value.trim();

    if (!newPassword || !confirmPassword) {
        alert(t("profile.messages.passwordRequired"));
        return;
    }

    if (newPassword !== confirmPassword) {
        alert(t("profile.messages.passwordsDoNotMatch"));
        return;
    }

    try {
        const response = await fetch(`http://localhost:8080/users/${currentUser.id}/password`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            credentials: "include",
            body: JSON.stringify({ newPassword })
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || "Password update failed");
        }

        alert(t("profile.messages.passwordUpdated"));

        document.getElementById("newPassword").value = "";
        document.getElementById("confirmPassword").value = "";

    } catch (error) {
        console.error(error);
        alert(t("profile.messages.passwordUpdateError"));
    }
}

// ======================
// DELETE ACCOUNT
// ======================

async function deleteAccount() {
    if (!confirm(t("profile.messages.confirmDeleteAccount"))) {
        return;
    }

    try {
        const response = await fetch(`http://localhost:8080/users/${currentUser.id}`, {
            method: "DELETE",
            credentials: "include"
        });

        if (!response.ok) {
            throw new Error("Delete failed");
        }

        await fetch("http://localhost:8080/auth/logout", {
            method: "POST",
            credentials: "include"
        });

        alert(t("profile.messages.accountDeleted"));
        window.location.href = "/";

    } catch (error) {
        console.error(error);
        alert(t("profile.messages.accountDeleteError"));
    }
}

// ======================
// LOAD ORDERS
// ======================

async function loadOrders() {
    const container = document.getElementById("ordersContainer");

    try {
        const response = await fetch("http://localhost:8080/orders/my", {
            credentials: "include"
        });

        if (!response.ok) {
            throw new Error("Failed to load orders");
        }

        currentOrders = await response.json();
        renderOrders(currentOrders);

    } catch (error) {
        console.error(error);
        container.innerHTML = `<p>${t("profile.messages.ordersLoadError")}</p>`;
    }
}

function renderOrders(orders) {
    const container = document.getElementById("ordersContainer");
    container.innerHTML = "";

    if (!orders || orders.length === 0) {
        container.innerHTML = `<p>${t("profile.noOrders")}</p>`;
        return;
    }

    orders.forEach(order => {
        const total = calculateOrderTotal(order.orderItems);

        const itemsHtml = order.orderItems.map(item => `
            <div class="order-item">
                <p><strong>${item.product.name}</strong> (${item.product.brand})</p>
                <p>${t("profile.order.quantity")}: ${item.quantity}</p>
                <p>${t("profile.order.unitPrice")}: ${Number(item.price_at_order_time).toFixed(2)} EUR</p>
                <p>${t("profile.order.subtotal")}: ${(Number(item.price_at_order_time) * item.quantity).toFixed(2)} EUR</p>
            </div>
        `).join("");

        const orderHtml = `
            <div class="order-card">
                <div class="order-header">
                    <h4>${t("profile.order.orderId")} #${order.id}</h4>
                    <span class="order-status status-${order.status.toLowerCase()}">
                        ${translateOrderStatus(order.status)}
                    </span>
                </div>

                <p><strong>${t("profile.order.date")}:</strong> ${formatDate(order.order_date)}</p>
                <p><strong>${t("profile.order.total")}:</strong> ${total.toFixed(2)} EUR</p>

                <div style="margin: 10px 0;">
                    <button onclick="downloadPdf(${order.id})">
                        ${t("profile.order.downloadPdf")}
                    </button>
                </div>

                <div class="order-items">
                    ${itemsHtml}
                </div>
            </div>
        `;

        container.innerHTML += orderHtml;
    });
}

// ======================
// PDF DOWNLOAD
// ======================

function downloadPdf(orderId) {
    window.open(`http://localhost:8080/orders/${orderId}/pdf`, "_blank");
}

// ======================
// HELPERS
// ======================

function calculateOrderTotal(orderItems) {
    if (!orderItems || orderItems.length === 0) return 0;

    return orderItems.reduce((sum, item) => {
        return sum + (Number(item.price_at_order_time) * item.quantity);
    }, 0);
}

function translateOrderStatus(status) {
    const key = `admin.statuses.${status}`;
    const translated = t(key);
    return translated === key ? status : translated;
}

function formatDate(dateString) {
    if (!dateString) return "-";
    const date = new Date(dateString);
    return date.toLocaleString(getCurrentLanguage() === "hr" ? "hr-HR" : "en-GB");
}

/*function loadUserThemePreference() {
    const select = document.getElementById("userTheme");
    if (!select) return;

    const savedTheme = localStorage.getItem("userTheme");
    select.value = savedTheme || "system";
}

function saveUserTheme() {
    const selectedTheme = document.getElementById("userTheme").value;

    if (selectedTheme === "system") {
        localStorage.removeItem("userTheme");
        applyAppTheme();
    } else {
        localStorage.setItem("userTheme", selectedTheme);
        setTheme(selectedTheme);
    }

    alert(t("profile.messages.themeSaved"));
}*/

function renderThemeSelection() {
    const currentTheme = localStorage.getItem("userTheme") || "light";

    const lightButton = document.getElementById("lightThemeButton");
    const darkButton = document.getElementById("darkThemeButton");

    if (!lightButton || !darkButton) return;

    lightButton.classList.toggle("active-theme-option", currentTheme === "light");
    darkButton.classList.toggle("active-theme-option", currentTheme === "dark");
}

function selectUserTheme(theme) {
    saveThemePreference(theme);
    renderThemeSelection();
}