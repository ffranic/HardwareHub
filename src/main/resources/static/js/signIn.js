document.addEventListener("DOMContentLoaded", () => {
    applyTranslations();

    document.addEventListener("languageChanged", () => {
        applyTranslations();
    });

    document.getElementById("signInForm").addEventListener("submit", handleSignIn);
});

async function handleSignIn(event) {
    event.preventDefault();

    const username = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value.trim();

    if (!username) {
        alert(t("signIn.messages.usernameRequired"));
        return;
    }

    if (!password) {
        alert(t("signIn.messages.passwordRequired"));
        return;
    }

    try {
        const response = await fetch("http://localhost:8080/auth/login", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            credentials: "include",
            body: JSON.stringify({ username, password })
        });

        const text = await response.text();
        let data = {};

        if (text) {
            try {
                data = JSON.parse(text);
            } catch (e) {
                console.error("Response is not valid JSON:", text);
                throw new Error("Invalid server response.");
            }
        }

        if (!response.ok) {
            throw new Error(data.error || t("signIn.messages.error"));
        }

        alert(t("signIn.messages.success"));

        // (ovo realno više ne treba, ali ostavljamo zbog kompatibilnosti)
        localStorage.setItem("loggedUser", JSON.stringify(data));

        window.location.href = "/html/index.html";

    } catch (error) {
        alert(error.message);
        console.error("Error while signing in:", error);
    }
}