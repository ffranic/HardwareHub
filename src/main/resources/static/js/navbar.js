document.addEventListener("DOMContentLoaded", async () => {
    if (typeof applyTranslations === "function") {
        applyTranslations();
    }

    document.addEventListener("languageChanged", () => {
        renderNavigation();
    });

    await renderNavigation();
});

async function renderNavigation() {
    const navList = document.getElementById("navList");
    if (!navList) return;

    let user = null;

    try {
        const response = await fetch("http://localhost:8080/auth/me", {
            credentials: "include"
        });

        if (response.ok) {
            user = await response.json();
        }
    } catch (error) {
        console.error("Failed to fetch current user:", error);
    }

    let navHTML = `
        <li><a href="../html/index.html">${t("nav.home")}</a></li>
    `;

    if (!user) {
        navHTML += `
            <li><a href="../html/registration.html">${t("nav.registration")}</a></li>
            <li><a href="../html/signIn.html">${t("nav.signIn")}</a></li>
        `;
    } else {
        navHTML += `
            <li><a href="../html/myProfile.html">${t("nav.myProfile")}</a></li>
            <li><a href="../html/cart.html">${t("nav.myCart")}</a></li>
        `;

        if (user.role === "ADMIN") {
            navHTML += `
                <li><a href="../html/administration.html">${t("nav.administration")}</a></li>
            `;
        }

        navHTML += `
            <li><a href="#" onclick="logOut(event)">${t("nav.logOut")}</a></li>
        `;
    }

    navHTML += `
        <li><a href="../html/contact.html">${t("nav.contact")}</a></li>

        <li class="nav-language-switcher">
            <span>${t("common.language")}:</span>
            <button type="button" onclick="setLanguage('en')" class="${getCurrentLanguage() === "en" ? "active-lang" : ""}">EN</button>
            <button type="button" onclick="setLanguage('hr')" class="${getCurrentLanguage() === "hr" ? "active-lang" : ""}">HR</button>
        </li>
    `;

    navList.innerHTML = navHTML;
}

async function logOut(event) {
    if (event) {
        event.preventDefault();
    }

    try {
        await fetch("http://localhost:8080/auth/logout", {
            method: "POST",
            credentials: "include"
        });
    } catch (err) {
        console.error("Logout failed:", err);
    } finally {
        window.location.href = "../html/index.html";
    }
}