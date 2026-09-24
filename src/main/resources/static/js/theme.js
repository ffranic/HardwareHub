document.addEventListener("DOMContentLoaded", () => {
    applyUserTheme();
});

function applyUserTheme() {
    const theme = localStorage.getItem("userTheme") || "light";
    setTheme(theme);
}

function setTheme(theme) {
    document.body.classList.remove("theme-light", "theme-dark");

    if (theme === "dark") {
        document.body.classList.add("theme-dark");
    } else {
        document.body.classList.add("theme-light");
    }
}

function saveThemePreference(theme) {
    localStorage.setItem("userTheme", theme);
    setTheme(theme);
}