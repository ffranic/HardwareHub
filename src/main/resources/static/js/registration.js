document.addEventListener("DOMContentLoaded", () => {
    applyTranslations();

    document.addEventListener("languageChanged", () => {
        applyTranslations();
    });

    document.getElementById("registrationForm").addEventListener("submit", registerUser);
});

async function registerUser(event) {
    event.preventDefault();

    const email = document.getElementById("email").value.trim();
    const username = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value.trim();

    if (!email) {
        alert(t("registration.messages.emailRequired"));
        return;
    }

    if (!username) {
        alert(t("registration.messages.usernameRequired"));
        return;
    }

    if (!password) {
        alert(t("registration.messages.passwordRequired"));
        return;
    }

    const formData = {
        email,
        username,
        password
    };

    try {
        const response = await fetch("http://localhost:8080/users", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(formData)
        });

        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || "Registration failed.");
        }

        alert(t("registration.messages.success"));
        window.location.href = "/html/signIn.html";

    } catch (error) {
        console.error("Registration error:", error);
        alert(t("registration.messages.error"));
    }
}