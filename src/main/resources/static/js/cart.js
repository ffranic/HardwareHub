let currentCart = null;

document.addEventListener("DOMContentLoaded", async function () {
    applyTranslations();

    document.addEventListener("languageChanged", () => {
        applyTranslations();

        if (currentCart) {
            renderCart(currentCart);
        }
    });

    await loadCart();
});

async function loadCart() {
    const container = document.getElementById("cart-items-container");
    const totalElem = document.getElementById("cart-total");

    try {
        const response = await fetch("http://localhost:8080/cart", {
            credentials: "include"
        });

        if (response.status === 401) {
            alert(t("cart.messages.notLoggedIn"));
            window.location.href = "/html/signIn.html";
            return;
        }

        if (!response.ok) {
            throw new Error("Failed to fetch cart");
        }

        currentCart = await response.json();
        renderCart(currentCart);

    } catch (error) {
        console.error("Error loading cart:", error);
        container.innerHTML = `<p>${t("cart.messages.loadError")}</p>`;
        totalElem.textContent = `${t("cart.totalPrice")}: 0.00 EUR`;
    }
}

function renderCart(cart) {
    const container = document.getElementById("cart-items-container");
    const totalElem = document.getElementById("cart-total");
    const checkoutBtn = document.getElementById("checkout-btn");

    container.innerHTML = "";

    if (!cart.items || cart.items.length === 0) {
        container.innerHTML = `<p>${t("cart.emptyCart")}</p>`;
        totalElem.textContent = `${t("cart.totalPrice")}: 0.00 EUR`;
        checkoutBtn.disabled = true;
        return;
    }

    checkoutBtn.disabled = false;

    let total = 0;

    cart.items.forEach(item => {
        const product = item.product;
        const price = parseFloat(product.price);
        const quantity = item.quantity;
        const itemTotal = price * quantity;

        total += itemTotal;

        const imageSrc = product.hasImage
            ? `http://localhost:8080/products/${product.id}/image`
            : "/images/product-placeholder.png";

        const itemHtml = `
            <div class="cart-item">
                <img src="${imageSrc}" alt="${product.name}">
                
                <div class="cart-item-info">
                    <h3>${product.name}</h3>
                    <p><strong>Brand:</strong> ${product.brand}</p>
                    <p><strong>${t("cart.price")}:</strong> ${price.toFixed(2)} EUR</p>
                    <p><strong>${t("cart.quantity")}:</strong> ${quantity}</p>
                    <p><strong>${t("cart.subtotal")}:</strong> ${itemTotal.toFixed(2)} EUR</p>
                </div>

                <div class="cart-item-actions">
                    <button onclick="changeQuantity(${item.id}, ${quantity - 1})">-</button>
                    <button onclick="changeQuantity(${item.id}, ${quantity + 1})">+</button>
                    <button onclick="removeFromCart(${item.id})">${t("cart.remove")}</button>
                </div>
            </div>
        `;

        container.innerHTML += itemHtml;
    });

    totalElem.textContent = `${t("cart.totalPrice")}: ${total.toFixed(2)} EUR`;
}

async function removeFromCart(itemId) {
    try {
        const response = await fetch(`http://localhost:8080/cart/items/${itemId}`, {
            method: "DELETE",
            credentials: "include"
        });

        if (!response.ok) {
            throw new Error("Failed to remove item");
        }

        await loadCart();

    } catch (error) {
        console.error("Error removing product:", error);
        alert(t("cart.messages.removeError"));
    }
}

async function changeQuantity(itemId, newQuantity) {
    try {
        if (newQuantity <= 0) {
            await removeFromCart(itemId);
            return;
        }

        const response = await fetch(`http://localhost:8080/cart/items/${itemId}`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            credentials: "include",
            body: JSON.stringify({ quantity: newQuantity })
        });

        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || "Failed to update quantity");
        }

        await loadCart();

    } catch (error) {
        console.error("Error updating quantity:", error);
        alert(t("cart.messages.quantityUpdateError"));
    }
}

async function proceedToCheckout() {
    try {
        const cartResponse = await fetch("http://localhost:8080/cart", {
            credentials: "include"
        });

        if (!cartResponse.ok) {
            throw new Error("Failed to load cart");
        }

        const cart = await cartResponse.json();

        if (!cart.items || cart.items.length === 0) {
            alert(t("cart.messages.emptyCartCheckout"));
            return;
        }

        const response = await fetch("http://localhost:8080/cart/checkout", {
            method: "POST",
            credentials: "include"
        });

        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || "Checkout failed");
        }

        const order = await response.json();

        alert(`${t("cart.messages.checkoutSuccess")} ${t("profile.order.orderId")}: ${order.id}`);
        window.location.href = "/html/myProfile.html";

    } catch (error) {
        console.error("Error during checkout:", error);
        alert(t("cart.messages.checkoutError"));
    }
}