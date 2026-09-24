let allUsers = [];
let allOrders = [];
let allCategories = [];
let allProducts = [];
let editingProductId = null;
let editingCategoryId = null;

document.addEventListener("DOMContentLoaded", async function () {
    applyTranslations();

    document.addEventListener("languageChanged", () => {
        applyTranslations();
        displayUsers(allUsers);
        displayOrders(allOrders);
        displayCategories(allCategories);
        displayProducts(allProducts);
        updateDynamicButtonLabels();
    });

    try {
        await checkAdminAccess();
        bindEventListeners();

        await Promise.all([
            loadSettings(),
            loadUsers(),
            loadOrders(),
            loadCategories(),
            loadProducts()
        ]);

        updateDynamicButtonLabels();

    } catch (error) {
        console.error(error);
        alert(t("admin.messages.accessDenied"));
        window.location.href = "/";
    }
});

function bindEventListeners() {
    document.getElementById("searchInput").addEventListener("input", filterUsers);
    document.getElementById("roleFilter").addEventListener("change", filterUsers);

    document.getElementById("categoryForm").addEventListener("submit", async function (event) {
        event.preventDefault();
        await saveCategory();
    });

    document.getElementById("productForm").addEventListener("submit", async function (event) {
        event.preventDefault();
        await saveProduct();
    });
}

function updateDynamicButtonLabels() {
    const categorySubmitButton = document.getElementById("categorySubmitButton");
    const cancelCategoryEditButton = document.getElementById("cancelCategoryEditButton");
    const productSubmitButton = document.getElementById("productSubmitButton");
    const cancelEditButton = document.getElementById("cancelEditButton");

    if (categorySubmitButton) {
        categorySubmitButton.textContent = editingCategoryId !== null
            ? t("common.saveChanges")
            : t("admin.addCategory");
    }

    if (cancelCategoryEditButton) {
        cancelCategoryEditButton.textContent = t("admin.cancelCategoryEdit");
    }

    if (productSubmitButton) {
        productSubmitButton.textContent = editingProductId !== null
            ? t("common.saveChanges")
            : t("admin.addProduct");
    }

    if (cancelEditButton) {
        cancelEditButton.textContent = t("admin.cancelProductEdit");
    }
}

async function checkAdminAccess() {
    const response = await fetch("http://localhost:8080/auth/me", {
        credentials: "include"
    });

    if (!response.ok) {
        throw new Error(t("admin.messages.notAuthenticated"));
    }

    const user = await response.json();

    if (user.role !== "ADMIN") {
        throw new Error(t("admin.messages.notAuthorized"));
    }
}

/* =========================
   USERS
========================= */

async function loadUsers() {
    try {
        const response = await fetch("http://localhost:8080/users", {
            credentials: "include"
        });

        if (!response.ok) {
            throw new Error("Failed to fetch users");
        }

        allUsers = await response.json();
        displayUsers(allUsers);

    } catch (error) {
        console.error("Error while fetching users:", error);
    }
}

function displayUsers(users) {
    const tbody = document.querySelector("#userTable tbody");
    tbody.innerHTML = "";

    if (!users || users.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6">${t("admin.userTable.noUsers")}</td></tr>`;
        return;
    }

    users.forEach(user => {
        const row = document.createElement("tr");

        row.innerHTML = `
            <td>${user.id}</td>
            <td>${user.username}</td>
            <td>${user.email ?? "-"}</td>
            <td>${user.role}</td>
            <td>
                <select onchange="updateUserRole(${user.id}, this.value)">
                    ${buildRoleOptions(user.role)}
                </select>
            </td>
            <td>
                <button class="danger-button" onclick="deleteUser(${user.id})">${t("common.delete")}</button>
            </td>
        `;

        tbody.appendChild(row);
    });
}

function buildRoleOptions(currentRole) {
    const roles = ["ADMIN", "CUSTOMER"];

    return roles.map(role => `
        <option value="${role}" ${role === currentRole ? "selected" : ""}>
            ${role === "ADMIN" ? t("admin.adminRole") : t("admin.customerRole")}
        </option>
    `).join("");
}

function filterUsers() {
    const searchValue = document.getElementById("searchInput").value.toLowerCase();
    const roleValue = document.getElementById("roleFilter").value;

    const filtered = allUsers.filter(user => {
        const matchesSearch =
            (user.username && user.username.toLowerCase().includes(searchValue)) ||
            (user.email && user.email.toLowerCase().includes(searchValue));

        const matchesRole = roleValue === "" || user.role === roleValue;

        return matchesSearch && matchesRole;
    });

    displayUsers(filtered);
}

async function updateUserRole(userId, newRole) {
    try {
        const response = await fetch(`http://localhost:8080/users/${userId}/role`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            credentials: "include",
            body: JSON.stringify({ role: newRole })
        });

        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || "Failed to update role");
        }

        await loadUsers();
        alert(t("admin.messages.userRoleUpdated"));

    } catch (error) {
        console.error("Role update error:", error);
        alert(t("admin.messages.userRoleUpdateError"));
    }
}

async function deleteUser(id) {
    if (!confirm(t("admin.messages.confirmDeleteUser"))) {
        return;
    }

    try {
        const response = await fetch(`http://localhost:8080/users/${id}`, {
            method: "DELETE",
            credentials: "include"
        });

        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || "Failed to delete user");
        }

        alert(t("admin.messages.userDeleted"));
        await loadUsers();

    } catch (error) {
        console.error("Delete error:", error);
        alert(t("admin.messages.userDeleteError"));
    }
}

/* =========================
   ORDERS
========================= */

async function loadOrders() {
    try {
        const response = await fetch("http://localhost:8080/orders/admin/orders", {
            credentials: "include"
        });

        if (!response.ok) {
            throw new Error("Failed to fetch orders");
        }

        allOrders = await response.json();
        displayOrders(allOrders);

    } catch (error) {
        console.error("Error while fetching orders:", error);
    }
}

function displayOrders(orders) {
    const tbody = document.querySelector("#orderTable tbody");
    tbody.innerHTML = "";

    if (!orders || orders.length === 0) {
        tbody.innerHTML = `<tr><td colspan="9">${t("admin.orderTable.noOrders")}</td></tr>`;
        return;
    }

    orders.forEach(order => {

        const total = calculateOrderTotal(order.orderItems);

        const row = document.createElement("tr");

        row.innerHTML = `
            <td>${order.id}</td>

            <td>${order.user?.username ?? "-"}</td>

            <td>${order.user?.email ?? "-"}</td>

            <td>${formatDate(order.order_date)}</td>

            <td>${translateStatus(order.status)}</td>

            <td>${total.toFixed(2)} EUR</td>

            <td id="signature-${order.id}">
                <span style="color:#777;">Not verified</span>
            </td>

            <td>
                <select onchange="updateOrderStatus(${order.id}, this.value)">
                    ${buildStatusOptions(order.status)}
                </select>
            </td>

            <td>
                <button onclick="verifyOrderSignature(${order.id})">
                    Verify
                </button>
            </td>
        `;

        tbody.appendChild(row);
    });
}

async function verifyOrderSignature(orderId) {

    try {

        const response = await fetch(
            `http://localhost:8080/digital-signature/orders/${orderId}/verify`,
            {
                credentials: "include"
            }
        );

        if (!response.ok) {
            throw new Error("Verification failed");
        }

        const result = await response.json();

        const cell = document.getElementById(`signature-${orderId}`);

        if (result.valid) {

            cell.innerHTML = `
                <span style="color:green;font-weight:bold;">
                    ✓ Valid
                </span>
            `;

        } else {

            cell.innerHTML = `
                <span style="color:red;font-weight:bold;">
                    ✗ Invalid
                </span>
            `;
        }

    } catch (error) {

        console.error(error);

        alert("Could not verify digital signature.");

    }
}

function buildStatusOptions(currentStatus) {
    const statuses = ["NEW", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED"];

    return statuses.map(status => `
        <option value="${status}" ${status === currentStatus ? "selected" : ""}>
            ${translateStatus(status)}
        </option>
    `).join("");
}

function translateStatus(status) {
    return t(`admin.statuses.${status}`);
}

async function updateOrderStatus(orderId, newStatus) {
    const terminalStatuses = ["DELIVERED", "CANCELLED"];

    if (terminalStatuses.includes(newStatus)) {
        const confirmed = confirm(
            "This status will close the order and move it to the binary archive. It will no longer appear in active Order Management. Continue?"
        );

        if (!confirmed) {
            await loadOrders();
            return;
        }
    }

    try {
        const response = await fetch(`http://localhost:8080/orders/${orderId}/status`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            credentials: "include",
            body: JSON.stringify({ status: newStatus })
        });

        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || "Failed to update status");
        }

        await loadOrders();

        if (terminalStatuses.includes(newStatus)) {
            alert("Order was closed and archived in the daily binary order archive.");
        } else {
            alert(t("admin.messages.orderStatusUpdated"));
        }

    } catch (error) {
        console.error("Status update error:", error);
        alert(t("admin.messages.orderStatusUpdateError"));
        await loadOrders();
    }
}

/* =========================
   CATEGORIES
========================= */

async function loadCategories() {
    try {
        const response = await fetch("http://localhost:8080/categories", {
            credentials: "include"
        });

        if (!response.ok) {
            throw new Error("Failed to fetch categories");
        }

        allCategories = await response.json();
        displayCategories(allCategories);
        populateCategoryDropdown(allCategories);

    } catch (error) {
        console.error("Error while fetching categories:", error);
    }
}

function displayCategories(categories) {
    const tbody = document.querySelector("#categoryTable tbody");
    tbody.innerHTML = "";

    if (!categories || categories.length === 0) {
        tbody.innerHTML = `<tr><td colspan="3">${t("admin.categoryTable.noCategories")}</td></tr>`;
        return;
    }

    categories.forEach(category => {
        const row = document.createElement("tr");
        row.innerHTML = `
            <td>${category.id}</td>
            <td>${category.name}</td>
            <td>
                <button onclick="editCategory(${category.id})">${t("common.edit")}</button>
                <button class="danger-button" onclick="deleteCategory(${category.id})">${t("common.delete")}</button>
            </td>
        `;
        tbody.appendChild(row);
    });
}

function populateCategoryDropdown(categories) {
    const select = document.getElementById("productCategory");
    select.innerHTML = `<option value="">${t("admin.selectCategory")}</option>`;

    categories.forEach(category => {
        const option = document.createElement("option");
        option.value = category.id;
        option.textContent = category.name;
        select.appendChild(option);
    });
}

async function editCategory(categoryId) {
    const category = allCategories.find(c => c.id === categoryId);

    if (!category) {
        alert(t("admin.messages.categoryNotFound"));
        return;
    }

    try {
        if (editingCategoryId !== null && editingCategoryId !== categoryId) {
            await releaseAdminEditLock("category", editingCategoryId);
        }

        await acquireAdminEditLock("category", categoryId);

        editingCategoryId = category.id;

        document.getElementById("editingCategoryId").value = category.id;
        document.getElementById("categoryName").value = category.name ?? "";
        document.getElementById("cancelCategoryEditButton").style.display = "inline-block";

        updateDynamicButtonLabels();

        window.scrollTo({
            top: document.getElementById("categoryForm").offsetTop - 20,
            behavior: "smooth"
        });

    } catch (error) {
        alert(error.message);
    }
}

async function cancelCategoryEdit() {
    if (editingCategoryId !== null) {
        await releaseAdminEditLock("category", editingCategoryId);
    }

    editingCategoryId = null;
    document.getElementById("categoryForm").reset();
    document.getElementById("editingCategoryId").value = "";
    document.getElementById("cancelCategoryEditButton").style.display = "none";
    updateDynamicButtonLabels();
}

async function saveCategory() {
    const nameInput = document.getElementById("categoryName");
    const name = nameInput.value.trim();

    if (!name) {
        alert(t("admin.messages.categoryNameRequired"));
        return;
    }

    try {
        let url = "http://localhost:8080/categories";
        let method = "POST";
        const wasEditing = editingCategoryId !== null;

        if (wasEditing) {
            url = `http://localhost:8080/categories/${editingCategoryId}`;
            method = "PUT";
        }

        const response = await fetch(url, {
            method: method,
            headers: {
                "Content-Type": "application/json"
            },
            credentials: "include",
            body: JSON.stringify({ name })
        });

        if (!response.ok) {
            const data = await response.json().catch(() => null);
            throw new Error(data?.error || "Failed to save category");
        }

        editingCategoryId = null;
        document.getElementById("categoryForm").reset();
        document.getElementById("editingCategoryId").value = "";
        document.getElementById("cancelCategoryEditButton").style.display = "none";
        updateDynamicButtonLabels();

        await loadCategories();

        alert(wasEditing ? t("admin.messages.categoryUpdated") : t("admin.messages.categoryAdded"));

    } catch (error) {
        console.error("Save category error:", error);
        alert(error.message || t("admin.messages.categorySaveError"));
    }
}

async function deleteCategory(id) {
    if (!confirm(t("admin.messages.confirmDeleteCategory"))) {
        return;
    }

    try {
        const response = await fetch(`http://localhost:8080/categories/${id}`, {
            method: "DELETE",
            credentials: "include"
        });

        if (!response.ok) {
            const data = await response.json().catch(() => null);
            throw new Error(data?.error || "Failed to delete category");
        }

        if (editingCategoryId === id) {
            editingCategoryId = null;
            document.getElementById("categoryForm").reset();
            document.getElementById("editingCategoryId").value = "";
            document.getElementById("cancelCategoryEditButton").style.display = "none";
            updateDynamicButtonLabels();
        }

        await loadCategories();
        alert(t("admin.messages.categoryDeleted"));

    } catch (error) {
        console.error("Delete category error:", error);
        alert(error.message || t("admin.messages.categoryDeleteError"));
    }
}

/* =========================
   PRODUCTS
========================= */

async function loadProducts() {
    try {
        const response = await fetch("http://localhost:8080/products", {
            credentials: "include"
        });

        if (!response.ok) {
            throw new Error("Failed to fetch products");
        }

        allProducts = await response.json();
        displayProducts(allProducts);

    } catch (error) {
        console.error("Error while fetching products:", error);
    }
}

function displayProducts(products) {
    const tbody = document.querySelector("#productTable tbody");
    tbody.innerHTML = "";

    if (!products || products.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8">${t("admin.productTable.noProducts")}</td></tr>`;
        return;
    }

    products.forEach(product => {
        const imageHtml = product.hasImage
            ? `<img class="product-thumb" src="http://localhost:8080/products/${product.id}/image" alt="${product.name}">`
            : `<span>${t("admin.productTable.noImage")}</span>`;

        const row = document.createElement("tr");

        row.innerHTML = `
            <td>${product.id}</td>
            <td>${product.name}</td>
            <td>${product.brand}</td>
            <td>${product.category?.name ?? "-"}</td>
            <td>${product.price}</td>
            <td>${product.stock}</td>
            <td>${imageHtml}</td>
            <td>
                <button onclick="editProduct(${product.id})">${t("common.edit")}</button>
                <button class="danger-button" onclick="deleteProduct(${product.id})">${t("common.delete")}</button>
            </td>
        `;

        tbody.appendChild(row);
    });
}

async function editProduct(productId) {
    const product = allProducts.find(p => p.id === productId);

    if (!product) {
        alert(t("admin.messages.productNotFound"));
        return;
    }

    try {
        if (editingProductId !== null && editingProductId !== productId) {
            await releaseAdminEditLock("product", editingProductId);
        }

        await acquireAdminEditLock("product", productId);

        editingProductId = product.id;

        document.getElementById("productId").value = product.id;
        document.getElementById("productName").value = product.name ?? "";
        document.getElementById("productBrand").value = product.brand ?? "";
        document.getElementById("productCategory").value = product.category?.id ?? "";
        document.getElementById("productPrice").value = product.price ?? "";
        document.getElementById("productStock").value = product.stock ?? "";
        document.getElementById("productDescription").value = product.description ?? "";

        document.getElementById("cancelEditButton").style.display = "inline-block";
        updateDynamicButtonLabels();

        window.scrollTo({
            top: document.getElementById("productForm").offsetTop - 20,
            behavior: "smooth"
        });

    } catch (error) {
        alert(error.message);
    }
}

async function cancelEdit() {
    if (editingProductId !== null) {
        await releaseAdminEditLock("product", editingProductId);
    }

    editingProductId = null;
    document.getElementById("productForm").reset();
    document.getElementById("productId").value = "";
    document.getElementById("cancelEditButton").style.display = "none";
    updateDynamicButtonLabels();
}

async function saveProduct() {
    const name = document.getElementById("productName").value.trim();
    const brand = document.getElementById("productBrand").value.trim();
    const categoryId = document.getElementById("productCategory").value;
    const price = document.getElementById("productPrice").value;
    const stock = document.getElementById("productStock").value;
    const description = document.getElementById("productDescription").value.trim();
    const image = document.getElementById("productImage").files[0];

    if (!name || !brand || !categoryId || !price || !stock) {
        alert(t("admin.messages.productRequiredFields"));
        return;
    }

    try {
        const formData = new FormData();
        formData.append("name", name);
        formData.append("brand", brand);
        formData.append("categoryId", categoryId);
        formData.append("price", price);
        formData.append("stock", stock);
        formData.append("description", description);

        if (image) {
            formData.append("image", image);
        }

        let url = "http://localhost:8080/products/with-image";
        let method = "POST";

        if (editingProductId !== null) {
            url = `http://localhost:8080/products/${editingProductId}/with-image`;
            method = "PUT";
        }

        const response = await fetch(url, {
            method: method,
            credentials: "include",
            body: formData
        });

        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || "Failed to save product");
        }

        const wasEditing = editingProductId !== null;

        editingProductId = null;
        document.getElementById("productForm").reset();
        document.getElementById("productId").value = "";
        document.getElementById("cancelEditButton").style.display = "none";
        updateDynamicButtonLabels();
        await loadProducts();

        alert(wasEditing ? t("admin.messages.productUpdated") : t("admin.messages.productAdded"));

    } catch (error) {
        console.error("Save product error:", error);
        alert(t("admin.messages.productSaveError"));
    }
}

async function deleteProduct(id) {
    if (!confirm(t("admin.messages.confirmDeleteProduct"))) {
        return;
    }

    try {
        const response = await fetch(`http://localhost:8080/products/${id}`, {
            method: "DELETE",
            credentials: "include"
        });

        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || "Failed to delete product");
        }

        await loadProducts();
        alert(t("admin.messages.productDeleted"));

    } catch (error) {
        console.error("Delete product error:", error);
        alert(t("admin.messages.productDeleteError"));
    }
}

document.getElementById("settingsForm").addEventListener("submit", async function (event) {
    event.preventDefault();
    await saveSettings();
});

/* =========================
   HELPERS
========================= */

async function acquireAdminEditLock(resourceType, resourceId) {
    const response = await fetch(`http://localhost:8080/admin-edit-locks/${resourceType}/${resourceId}/acquire`, {
        method: "POST",
        credentials: "include"
    });

    if (!response.ok) {
        const data = await response.json().catch(() => null);
        throw new Error(data?.error || "This item is currently being edited by another admin.");
    }
}

async function releaseAdminEditLock(resourceType, resourceId) {
    await fetch(`http://localhost:8080/admin-edit-locks/${resourceType}/${resourceId}/release`, {
        method: "DELETE",
        credentials: "include"
    });
}

function calculateOrderTotal(orderItems) {
    if (!orderItems || orderItems.length === 0) return 0;

    return orderItems.reduce((sum, item) => {
        return sum + (Number(item.price_at_order_time) * item.quantity);
    }, 0);
}

function formatDate(dateString) {
    if (!dateString) return "-";
    return new Date(dateString).toLocaleString(getCurrentLanguage() === "hr" ? "hr-HR" : "en-GB");
}

async function loadSettings() {
    try {
        const response = await fetch("http://localhost:8080/settings", {
            credentials: "include"
        });

        if (!response.ok) {
            throw new Error("Failed to load settings");
        }

        const settings = await response.json();

        document.getElementById("settingsLanguage").value = settings.language;
        document.getElementById("settingsItemsPerPage").value = settings.itemsPerPage;

    } catch (error) {
        console.error("Settings load error:", error);
        alert(t("admin.messages.settingsLoadError"));
    }
}

async function saveSettings() {
    const settings = {
        language: document.getElementById("settingsLanguage").value,
        itemsPerPage: Number(document.getElementById("settingsItemsPerPage").value)
    };

    try {
        const response = await fetch("http://localhost:8080/settings", {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            credentials: "include",
            body: JSON.stringify(settings)
        });

        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || "Failed to save settings");
        }

        localStorage.setItem("appLanguage", settings.language);
        setLanguage(settings.language);

        alert(t("admin.messages.settingsSaved"));

    } catch (error) {
        console.error("Settings save error:", error);
        alert(t("admin.messages.settingsSaveError"));
    }
}

/* =========================
   SYSTEM MAINTENANCE & SECURITY
========================= */

function escapeHtml(value) {
    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function prettyPrint(data) {
    if (typeof data === "string") return data;
    return JSON.stringify(data, null, 2);
}

function tryParseJson(data) {
    if (typeof data !== "string") return data;

    try {
        return JSON.parse(data);
    } catch {
        return data;
    }
}

function showLoading(title) {
    const output = document.getElementById("systemToolsOutput");
    if (!output) return;

    output.innerHTML = `
        <div class="tool-result-card">
            <div class="tool-result-header">
                <div>
                    <h5>${escapeHtml(title)}</h5>
                    <p>Naredba se izvršava...</p>
                </div>
                <span class="tool-badge warning">U tijeku</span>
            </div>
        </div>
    `;
}

function showToolOutput(data, options = {}) {
    const output = document.getElementById("systemToolsOutput");
    if (!output) return;

    const parsedData = tryParseJson(data);
    const title = options.title || "Rezultat";
    const description = options.description || "Naredba je uspješno izvršena.";
    const type = options.type || "generic";

    let contentHtml;

    if (type === "activityLogs") {
        contentHtml = renderActivityLogs(parsedData);
    } else if (type === "archivedActivityLogs") {
        contentHtml = renderArchivedActivityLogs(parsedData);
    } else if (type === "binaryOrderArchive") {
        contentHtml = renderBinaryOrderArchive(parsedData);
    } else if (type === "auditIntegrity") {
        contentHtml = renderAuditIntegrity(parsedData);
    } else if (type === "hash") {
        contentHtml = renderHash(parsedData);
    } else if (type === "exchangeRate") {
        contentHtml = renderExchangeRate(parsedData);
    } else if (type === "inventorySnapshot") {
    contentHtml = renderInventorySnapshot(parsedData);
    } else if (type === "xml") {
    contentHtml = renderXml(parsedData);
    } else if (type === "systemAnalysis") {
        contentHtml = renderSystemAnalysis(parsedData);
    } else if (type === "binary") {
        contentHtml = renderBinary(parsedData);
    } else if (type === "pdfBatch") {
        contentHtml = renderPdfBatch(parsedData);
    } else if (type === "pdfBenchmark") {
        contentHtml = renderPdfBenchmark(parsedData);
    } else if (type === "signatureVerificationReport") {
        contentHtml = renderSignatureVerificationReport(parsedData);
    } else {
        contentHtml = renderGeneric(parsedData);
    }

    output.innerHTML = `
        <div class="tool-result-card">
            <div class="tool-result-header">
                <div>
                    <h5>${escapeHtml(title)}</h5>
                    <p>${escapeHtml(description)}</p>
                </div>
                <span class="tool-badge success">Uspješno</span>
            </div>

            <div class="tool-result-body">
                ${contentHtml}
            </div>

            <details class="tool-raw">
                <summary>Prikaži raw podatke</summary>
                <pre>${escapeHtml(prettyPrint(parsedData))}</pre>
            </details>
        </div>
    `;
}

function showToolError(error, title = "Greška") {
    const output = document.getElementById("systemToolsOutput");
    if (!output) return;

    output.innerHTML = `
        <div class="tool-result-card">
            <div class="tool-result-header">
                <div>
                    <h5>${escapeHtml(title)}</h5>
                    <p>${escapeHtml(error.message || "Došlo je do greške.")}</p>
                </div>
                <span class="tool-badge error">Greška</span>
            </div>
        </div>
    `;
}

function renderActivityLogs(data) {
    if (!Array.isArray(data)) {
        return renderGeneric(data);
    }

    if (data.length === 0) {
        return `<div class="tool-empty-message">Nema activity log zapisa.</div>`;
    }

    const rows = data.slice(0, 25).map(log => {
        const keys = Object.keys(log);

        const timeKey = keys.find(k =>
            k.toLowerCase().includes("time") ||
            k.toLowerCase().includes("date") ||
            k.toLowerCase().includes("timestamp")
        );

        const userKey = keys.find(k =>
            k.toLowerCase().includes("user") ||
            k.toLowerCase().includes("email") ||
            k.toLowerCase().includes("username")
        );

        const actionKey = keys.find(k =>
            k.toLowerCase().includes("action") ||
            k.toLowerCase().includes("event") ||
            k.toLowerCase().includes("activity") ||
            k.toLowerCase().includes("message")
        );

        return `
            <tr>
                <td>${escapeHtml(timeKey ? log[timeKey] : "-")}</td>
                <td>${escapeHtml(userKey ? log[userKey] : "-")}</td>
                <td>${escapeHtml(actionKey ? log[actionKey] : "-")}</td>
                <td>${escapeHtml(JSON.stringify(log))}</td>
            </tr>
        `;
    }).join("");

    return `
        <div class="tool-metrics">
            <div class="tool-metric">
                <span>Ukupno logova</span>
                <strong>${data.length}</strong>
            </div>
            <div class="tool-metric">
                <span>Prikazano</span>
                <strong>${Math.min(data.length, 25)}</strong>
            </div>
        </div>

        <div class="tool-table-wrapper">
            <table class="tool-result-table">
                <thead>
                    <tr>
                        <th>Vrijeme</th>
                        <th>Korisnik</th>
                        <th>Akcija</th>
                        <th>Detalji</th>
                    </tr>
                </thead>
                <tbody>${rows}</tbody>
            </table>
        </div>

        ${data.length > 25 ? `<p class="tool-note">Prikazano je prvih 25 zapisa. Cijeli sadržaj je dostupan u raw prikazu.</p>` : ""}
    `;
}

function renderHash(data) {
    let hash = "";

    if (typeof data === "string") {
        hash = data;
    } else if (data && typeof data === "object") {
        hash =
            data.hash ||
            data.sha256 ||
            data.digest ||
            data.value ||
            data.result ||
            Object.values(data).find(v => typeof v === "string" && v.length > 20) ||
            prettyPrint(data);
    }

    return `
        <div class="tool-message success">
            Hash je generiran. Koristi se za provjeru je li datoteka promijenjena.
        </div>

        <label class="tool-readonly-label">
            Hash vrijednost
            <input type="text" readonly value="${escapeHtml(hash)}">
        </label>
    `;
}

function renderExchangeRate(data) {
    let rate = "-";

    if (typeof data === "number") {
        rate = data;
    } else if (typeof data === "string") {
        rate = data;
    } else if (data && typeof data === "object") {
        rate =
            data.rate ||
            data.exchangeRate ||
            data.result ||
            data.eurUsd ||
            data.EUR_USD ||
            Object.values(data).find(v => typeof v === "number" || typeof v === "string") ||
            "-";
    }

    return `
        <div class="tool-highlight">
            <span>EUR/USD</span>
            <strong>${escapeHtml(rate)}</strong>
            <p>Podatak je dohvaćen iz vanjskog REST servisa.</p>
        </div>
    `;
}

function renderXml(data) {
    const text = prettyPrint(data);
    const productCount =
        (text.match(/<product/gi) || []).length ||
        (text.match(/<item/gi) || []).length ||
        "-";

    return `
        <div class="tool-metrics">
            <div class="tool-metric">
                <span>Format</span>
                <strong>XML</strong>
            </div>
            <div class="tool-metric">
                <span>Procjena zapisa</span>
                <strong>${escapeHtml(productCount)}</strong>
            </div>
            <div class="tool-metric">
                <span>Duljina sadržaja</span>
                <strong>${text.length}</strong>
            </div>
        </div>

        <div class="tool-message success">
            XML je uspješno obrađen. Cijeli XML možeš otvoriti u raw prikazu.
        </div>
    `;
}

function renderBinary(data) {
    const text = prettyPrint(data);

    return `
        <div class="tool-metrics">
            <div class="tool-metric">
                <span>Tip</span>
                <strong>Binary backup</strong>
            </div>
            <div class="tool-metric">
                <span>Duljina prikaza</span>
                <strong>${text.length}</strong>
            </div>
        </div>

        <div class="tool-message success">
            Binary backup naredba je izvršena. Detalji su dostupni u raw prikazu.
        </div>
    `;
}

function renderPdfBatch(data) {
    if (!data || typeof data !== "object") {
        return renderGeneric(data);
    }

    const ordersProcessed = Number(data.ordersProcessed ?? 0);

    if (ordersProcessed === 0) {
        return `
            <div class="tool-message warning">
                No PDF confirmations were generated because there are no orders in the system.
            </div>
        `;
    }

    return `
        <div class="tool-metrics">
            <div class="tool-metric">
                <span>Generated confirmations</span>
                <strong>${escapeHtml(data.ordersProcessed ?? "-")}</strong>
            </div>
            <div class="tool-metric">
                <span>Thread pool size</span>
                <strong>${escapeHtml(data.threadPoolSize ?? "-")}</strong>
            </div>
            <div class="tool-metric">
                <span>Total processing time</span>
                <strong>${escapeHtml(data.durationMs ?? "-")} ms</strong>
            </div>
        </div>

        <div class="tool-message success">
            PDF confirmations were successfully generated and packaged into a ZIP archive.
        </div>

        <div class="tool-detail-list">
            <div class="tool-detail-row">
                <span>Purpose</span>
                <strong>Administrative order documentation</strong>
            </div>
            <div class="tool-detail-row">
                <span>Processing model</span>
                <strong>Parallel PDF generation using Thread Pool</strong>
            </div>
        </div>
    `;
}

function renderPdfBenchmark(data) {
    if (!data || typeof data !== "object") {
        return renderGeneric(data);
    }

    const ordersProcessed = Number(data.ordersProcessed ?? 0);
    const sequential = Number(data.sequentialDurationMs ?? 0);
    const parallel = Number(data.parallelDurationMs ?? 0);

    if (ordersProcessed === 0) {
        return `
            <div class="tool-message warning">
                Performance analysis could not be executed because there are no orders in the system.
            </div>
        `;
    }

    let speedup = "-";
    let improvement = "-";

    if (sequential > 0 && parallel > 0) {
        speedup = (sequential / parallel).toFixed(2) + "x";
        improvement = (((sequential - parallel) / sequential) * 100).toFixed(1) + "%";
    }

    const faster = data.fasterWithThreadPool === true || (sequential > parallel && parallel > 0);

    return `
        <div class="tool-metrics">
            <div class="tool-metric">
                <span>Orders tested</span>
                <strong>${escapeHtml(data.ordersProcessed ?? "-")}</strong>
            </div>
            <div class="tool-metric">
                <span>Sequential processing</span>
                <strong>${escapeHtml(data.sequentialDurationMs ?? "-")} ms</strong>
            </div>
            <div class="tool-metric">
                <span>Thread-pool processing</span>
                <strong>${escapeHtml(data.parallelDurationMs ?? "-")} ms</strong>
            </div>
            <div class="tool-metric">
                <span>Thread pool size</span>
                <strong>${escapeHtml(data.threadPoolSize ?? "-")}</strong>
            </div>
            <div class="tool-metric">
                <span>Speed-up</span>
                <strong>${escapeHtml(speedup)}</strong>
            </div>
            <div class="tool-metric">
                <span>Time reduction</span>
                <strong>${escapeHtml(improvement)}</strong>
            </div>
        </div>

        <div class="tool-message ${faster ? "success" : "warning"}">
            ${
        faster
            ? "Thread-pool processing completed faster than sequential processing."
            : "Thread-pool processing was not faster in this run. This can happen with a small number of orders or low processing load."
    }
        </div>

        <div class="tool-detail-list">
            <div class="tool-detail-row">
                <span>Demonstrated concept</span>
                <strong>Parallel execution of independent PDF generation tasks</strong>
            </div>
            <div class="tool-detail-row">
                <span>Reason for using Thread Pool</span>
                <strong>Multiple order confirmations can be generated at the same time instead of one after another</strong>
            </div>
        </div>
    `;
}

function renderGeneric(data) {
    if (typeof data === "string") {
        return `<div class="tool-message success">${escapeHtml(data)}</div>`;
    }

    if (data && typeof data === "object") {
        const entries = Object.entries(data);

        if (entries.length === 0) {
            return `<div class="tool-empty-message">Naredba je izvršena, ali nema dodatnih podataka.</div>`;
        }

        return `
            <div class="tool-detail-list">
                ${entries.map(([key, value]) => `
                    <div class="tool-detail-row">
                        <span>${escapeHtml(key)}</span>
                        <strong>${escapeHtml(typeof value === "object" ? JSON.stringify(value) : value)}</strong>
                    </div>
                `).join("")}
            </div>
        `;
    }

    return `<div class="tool-message success">${escapeHtml(data ?? "Naredba je izvršena.")}</div>`;
}

async function callToolEndpoint(url, options = {}) {
    try {
        const response = await fetch(url, {
            credentials: "include",
            ...options,
            headers: {
                ...(options.headers || {})
            }
        });

        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || "Request failed");
        }

        const contentType = response.headers.get("Content-Type") || "";

        if (contentType.includes("application/json")) {
            return await response.json();
        }

        return await response.text();

    } catch (error) {
        console.error("System tool error:", error);
        showToolError(error);
        alert(t("admin.messages.systemToolError"));
        throw error;
    }
}

async function loadExchangeRate() {
    showLoading("EUR/USD tečaj");
    const data = await callToolEndpoint("http://localhost:8080/external/exchange-rate/eur-usd");

    showToolOutput(data, {
        title: "EUR/USD tečaj",
        description: "Dohvaćen je tečaj iz vanjskog REST servisa.",
        type: "exchangeRate"
    });
}

async function writeBinaryStockBackup() {
    showLoading("Write binary backup");
    const data = await callToolEndpoint("http://localhost:8080/binary-stock/write", {
        method: "POST"
    });

    showToolOutput(data, {
        title: "Write binary backup",
        description: "Binary backup zaliha je kreiran.",
        type: "binary"
    });
}

async function readBinaryStockBackup() {
    showLoading("Read binary backup");
    const data = await callToolEndpoint("http://localhost:8080/binary-stock/read");

    showToolOutput(data, {
        title: "Read binary backup",
        description: "Binary backup zaliha je pročitan.",
        type: "binary"
    });
}

async function generatePdfBatch() {
    try {
        showLoading("Generating order confirmations");

        const response = await fetch("http://localhost:8080/admin/pdf-batch/generate", {
            method: "POST",
            credentials: "include"
        });

        if (!response.ok) {
            throw new Error("Failed to generate order confirmations");
        }

        const data = await response.json();

        showToolOutput(data, {
            title: "Order confirmations",
            description: data.ordersProcessed === 0
                ? "There are no orders available for PDF confirmation generation."
                : "PDF confirmations were generated and stored in a ZIP archive.",
            type: "pdfBatch"
        });

    } catch (error) {
        console.error(error);
        showToolError(error, "Order confirmations");
        alert("Order confirmation generation failed.");
    }
}

async function benchmarkPdfBatch() {
    try {
        showLoading("Running performance analysis");

        const response = await fetch("http://localhost:8080/admin/pdf-batch/benchmark", {
            method: "POST",
            credentials: "include"
        });

        if (!response.ok) {
            throw new Error("Performance analysis failed");
        }

        const data = await response.json();

        showToolOutput(data, {
            title: "Performance analysis",
            description: data.ordersProcessed === 0
                ? "There are no orders available for performance analysis."
                : "Sequential and thread-pool PDF generation were compared.",
            type: "pdfBenchmark"
        });

    } catch (error) {
        console.error(error);
        showToolError(error, "Performance analysis");
        alert("Performance analysis failed.");
    }
}

async function loadRecentActivityLogs() {
    showLoading("Recent activity logs");

    const data = await callToolEndpoint("http://localhost:8080/activity-logs/current");

    showToolOutput(data, {
        title: "Recent activity",
        description: "Recent activity logs were decrypted in memory and displayed in a structured table.",
        type: "activityLogs"
    });
}

async function loadArchivedActivityLogs() {
    showLoading("Archived activity logs");

    const data = await callToolEndpoint("http://localhost:8080/activity-logs/archive");

    showToolOutput(data, {
        title: "Archived activity",
        description: "Archived activity logs are displayed by archive date.",
        type: "archivedActivityLogs"
    });
}

async function verifyAuditIntegrity() {
    showLoading("Audit integrity verification");

    const data = await callToolEndpoint("http://localhost:8080/activity-logs/integrity");

    showToolOutput(data, {
        title: "Audit integrity verification",
        description: "SHA-256 hashes were checked for current and archived encrypted log files.",
        type: "auditIntegrity"
    });
}

function renderArchivedActivityLogs(data) {
    if (!data || typeof data !== "object") {
        return renderGeneric(data);
    }

    const entries = Object.entries(data);

    if (entries.length === 0) {
        return `<div class="tool-empty-message">Nema arhiviranih activity logova.</div>`;
    }

    return entries.map(([date, logs]) => {
        const rows = Array.isArray(logs)
            ? logs.map(log => `
                <tr>
                    <td>${escapeHtml(log.timestamp ?? "-")}</td>
                    <td>${escapeHtml(log.username ?? "-")}</td>
                    <td>${escapeHtml(log.role ?? "-")}</td>
                    <td>${escapeHtml(log.action ?? "-")}</td>
                    <td>${escapeHtml(log.details ?? "-")}</td>
                    <td>${escapeHtml(log.ipAddress ?? "-")}</td>
                </tr>
            `).join("")
            : "";

        return `
            <div class="tool-archive-day">
                <h5>${escapeHtml(date)}</h5>
                <div class="tool-table-wrapper">
                    <table class="tool-result-table">
                        <thead>
                        <tr>
                            <th>Vrijeme</th>
                            <th>Korisnik</th>
                            <th>Uloga</th>
                            <th>Akcija</th>
                            <th>Detalji</th>
                            <th>IP adresa</th>
                        </tr>
                        </thead>
                        <tbody>${rows}</tbody>
                    </table>
                </div>
            </div>
        `;
    }).join("");
}

function renderAuditIntegrity(data) {
    if (!data || typeof data !== "object") {
        return renderGeneric(data);
    }

    const currentValid = data.currentLogValid === true;

    const archiveEntries = data.archives && typeof data.archives === "object"
        ? Object.entries(data.archives)
        : [];

    return `
        <div class="tool-metrics">
            <div class="tool-metric">
                <span>Current log</span>
                <strong>${currentValid ? "Valid" : "Invalid"}</strong>
            </div>
            <div class="tool-metric">
                <span>Archived files checked</span>
                <strong>${archiveEntries.length}</strong>
            </div>
        </div>

        <div class="tool-message ${currentValid ? "success" : "error"}">
            Current encrypted audit log integrity is ${currentValid ? "valid" : "invalid"}.
        </div>

        ${
        archiveEntries.length > 0
            ? `
                    <div class="tool-detail-list">
                        ${archiveEntries.map(([file, valid]) => `
                            <div class="tool-detail-row">
                                <span>${escapeHtml(file)}</span>
                                <strong>${valid ? "Valid" : "Invalid"}</strong>
                            </div>
                        `).join("")}
                    </div>
                `
            : `<div class="tool-empty-message">Nema arhivskih log datoteka za provjeru.</div>`
    }
    `;
}

async function loadInventorySnapshot() {
    showLoading("Inventory XML snapshot");

    const data = await callToolEndpoint("http://localhost:8080/stock-xml/snapshot");

    showToolOutput(data, {
        title: "Inventory XML snapshot",
        description: "XML snapshot was refreshed from the database and displayed in a structured view.",
        type: "inventorySnapshot"
    });
}

async function verifyInventorySnapshotIntegrity() {
    showLoading("Inventory snapshot integrity");

    const data = await callToolEndpoint("http://localhost:8080/crypto/hash/product-stock");

    showToolOutput(data, {
        title: "Inventory snapshot integrity",
        description: "SHA-256 hash was generated to verify whether product-stock.xml was modified.",
        type: "hash"
    });
}

function renderInventorySnapshot(data) {
    if (!Array.isArray(data)) {
        return renderGeneric(data);
    }

    if (data.length === 0) {
        return `<div class="tool-empty-message">Nema proizvoda u XML snapshotu.</div>`;
    }

    const totalStock = data.reduce((sum, item) => sum + Number(item.stock ?? 0), 0);

    const rows = data.map(item => `
        <tr>
            <td>${escapeHtml(item.productId ?? "-")}</td>
            <td>${escapeHtml(item.productName ?? "-")}</td>
            <td>${escapeHtml(item.brand ?? "-")}</td>
            <td>${escapeHtml(item.categoryName ?? "-")}</td>
            <td>${escapeHtml(item.price ?? "-")}</td>
            <td>${escapeHtml(item.stock ?? "-")}</td>
        </tr>
    `).join("");

    return `
        <div class="tool-metrics">
            <div class="tool-metric">
                <span>Products in snapshot</span>
                <strong>${data.length}</strong>
            </div>
            <div class="tool-metric">
                <span>Total stock units</span>
                <strong>${totalStock}</strong>
            </div>
            <div class="tool-metric">
                <span>Format</span>
                <strong>XML</strong>
            </div>
        </div>

        <div class="tool-table-wrapper">
            <table class="tool-result-table">
                <thead>
                <tr>
                    <th>ID</th>
                    <th>Product</th>
                    <th>Brand</th>
                    <th>Category</th>
                    <th>Price</th>
                    <th>Stock</th>
                </tr>
                </thead>
                <tbody>${rows}</tbody>
            </table>
        </div>

        <div class="tool-message success">
            Snapshot je automatski osvježen iz baze i zapisan u product-stock.xml.
        </div>
    `;
}

async function loadBinaryOrderArchive() {
    showLoading("Order binary archive");

    const data = await callToolEndpoint("http://localhost:8080/order-archive");

    showToolOutput(data, {
        title: "Order binary archive",
        description: "Completed and cancelled orders were read from daily binary archive files.",
        type: "binaryOrderArchive"
    });
}

function renderBinaryOrderArchive(data) {
    if (!data || typeof data !== "object") {
        return renderGeneric(data);
    }

    const archiveEntries = Object.entries(data);

    if (archiveEntries.length === 0) {
        return `<div class="tool-empty-message">Nema arhiviranih narudžbi.</div>`;
    }

    const archiveHtml = archiveEntries.map(([date, records]) => {

        if (!Array.isArray(records) || records.length === 0) {
            return "";
        }

        const rows = records.map(record => `
            <tr>
                <td>${escapeHtml(record.orderId ?? "-")}</td>
                <td>${escapeHtml(record.username ?? "-")}</td>
                <td>${escapeHtml(record.email ?? "-")}</td>
                <td>${escapeHtml(formatDate(record.orderDate) ?? "-")}</td>
                <td>${escapeHtml(formatDate(record.archivedAt) ?? "-")}</td>
                <td>${escapeHtml(record.status ?? "-")}</td>
                <td>${escapeHtml(record.total ?? "0")} EUR</td>

                <td id="signature-${record.orderId}">
                    <span style="color:#777;">Not verified</span>
                </td>

                <td>
                    <button type="button" onclick="verifyOrderSignature(${record.orderId})">
                        Verify
                    </button>
                </td>
            </tr>
        `).join("");

        return `
            <div class="tool-archive-day">

                <h5>${escapeHtml(date)}</h5>

                <div class="tool-table-wrapper">
                    <table class="tool-result-table">

                        <thead>
                            <tr>
                                <th>Order ID</th>
                                <th>User</th>
                                <th>Email</th>
                                <th>Order date</th>
                                <th>Archived at</th>
                                <th>Status</th>
                                <th>Total</th>
                                <th>Signature</th>
                                <th>Actions</th>
                            </tr>
                        </thead>

                        <tbody>
                            ${rows}
                        </tbody>

                    </table>
                </div>

            </div>
        `;
    }).join("");

    return `
        <div style="margin-bottom:20px;">
            <button type="button" onclick="verifyAllOrderSignatures()">
                Verify all signatures
            </button>
        </div>

        ${archiveHtml}
    `;
}

async function verifyAllOrderSignatures() {

    try {

        const response = await fetch(
            "http://localhost:8080/digital-signature/orders/verify-all",
            {
                credentials: "include"
            }
        );

        if (!response.ok) {
            throw new Error("Verification failed");
        }

        const result = await response.json();

        result.results.forEach(signature => {

            const cell = document.getElementById(`signature-${signature.orderId}`);

            if (!cell) return;

            cell.innerHTML = signature.valid
                ? `<span style="color:green;font-weight:bold;">✓ Valid</span>`
                : `<span style="color:red;font-weight:bold;">✗ Invalid</span>`;
        });

        alert(
            `Verified ${result.totalChecked} signatures using ${result.threadPoolSize} threads in ${result.durationMs} ms.`
        );

    } catch (e) {

        console.error(e);
        alert("Could not verify signatures.");

    }
}

async function viewSignatureVerificationReport() {

    try {

        const response = await fetch(
            "http://localhost:8080/digital-signature/report",
            {
                credentials: "include"
            }
        );

        if (!response.ok) {
            throw new Error();
        }

        const report = await response.json();

        showToolOutput(report, {
            title: "Signature verification report",
            description: "Forensic JSON verification report.",
            type: "signatureVerificationReport"
        });

    } catch (e) {

        alert("Verification report not found.");

    }

}

async function deleteSignatureVerificationReport() {

    if (!confirm("Delete verification report?")) {
        return;
    }

    try {

        const response = await fetch(
            "http://localhost:8080/digital-signature/report",
            {
                method: "DELETE",
                credentials: "include"
            }
        );

        if (!response.ok) {
            throw new Error();
        }

        alert("Verification report deleted.");

    } catch (e) {

        alert("Could not delete report.");

    }

}

function renderSignatureVerificationReport(report) {

    if (!report || !report.orders) {
        return `<div class="tool-empty-message">No verification report.</div>`;
    }

    return `

        <div class="tool-detail-list">

            <div class="tool-detail-row">
                <span>Generated</span>
                <strong>${escapeHtml(report.generatedAt)}</strong>
            </div>

            <div class="tool-detail-row">
                <span>Algorithm</span>
                <strong>${escapeHtml(report.algorithm)}</strong>
            </div>

            <div class="tool-detail-row">
                <span>Total</span>
                <strong>${report.totalOrders}</strong>
            </div>

            <div class="tool-detail-row">
                <span>Valid</span>
                <strong style="color:green">${report.validOrders}</strong>
            </div>

            <div class="tool-detail-row">
                <span>Invalid</span>
                <strong style="color:red">${report.invalidOrders}</strong>
            </div>

        </div>

        <br>

        ${report.orders.map(order => `

            <details style="margin-bottom:15px;">

                <summary>

                    Order ${order.orderId}

                    ${order.valid
        ? '<span style="color:green;">✓ VALID</span>'
        : '<span style="color:red;">✗ INVALID</span>'}

                </summary>

                <br>

                <b>Original content</b>

                <pre>${escapeHtml(order.originalContent)}</pre>

                <b>Current content</b>

                <pre>${escapeHtml(order.currentContent)}</pre>

                <b>Differences</b>

                <ul>

                    ${order.differences.map(diff =>
        `<li>${escapeHtml(diff)}</li>`
    ).join("")}

                </ul>

            </details>

        `).join("")}

    `;
}

async function runSystemAnalysis() {
    try {
        const response = await fetch("http://localhost:8080/analysis/system", {
            method: "POST",
            credentials: "include"
        });

        if (!response.ok) {
            throw new Error("System analysis failed");
        }

        const result = await response.json();

        showToolOutput(result, {
            title: "System analysis",
            description: "Parallel analysis completed using Thread Pool and ReentrantLock.",
            type: "systemAnalysis"
        });

    } catch (error) {
        console.error(error);
        alert("Could not run system analysis.");
    }
}

function renderSystemAnalysis(data) {
    if (!data || typeof data !== "object") {
        return renderGeneric(data);
    }

    return `
        <div class="tool-detail-list">
            <div class="tool-detail-row">
                <span>Output file</span>
                <strong>${escapeHtml(data.outputFile ?? "-")}</strong>
            </div>

            <div class="tool-detail-row">
                <span>Thread pool size</span>
                <strong>${escapeHtml(data.threadPoolSize ?? "-")}</strong>
            </div>

            <div class="tool-detail-row">
                <span>Duration</span>
                <strong>${escapeHtml(data.durationMs ?? "-")} ms</strong>
            </div>
        </div>

        <br>

        <pre>${escapeHtml(data.content ?? "")}</pre>
    `;
}