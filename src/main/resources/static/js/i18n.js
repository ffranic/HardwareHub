/*
 * ============================================================
 * I18N.JS
 * ============================================================
 *
 * Ova datoteka predstavlja centralnu logiku za internacionalizaciju
 * korisničkog sučelja.
 *
 * i18n = internationalization
 *
 * Slovo "i" + 18 slova između + "n".
 *
 * Aplikacija podržava:
 *
 * - English ("en")
 * - Hrvatski ("hr")
 *
 * Ova datoteka NE sadrži same prijevode.
 *
 * Prijevodi se nalaze u translations.js:
 *
 * window.translations = {
 *     en: {...},
 *     hr: {...}
 * }
 *
 * Ova datoteka:
 *
 * 1. određuje trenutni jezik
 * 2. sprema izbor jezika u localStorage
 * 3. dohvaća prijevod prema ključu
 * 4. primjenjuje prijevode na HTML elemente
 * 5. obavještava ostatak aplikacije o promjeni jezika
 */


/*
 * IIFE - Immediately Invoked Function Expression.
 *
 * Funkcija se definira i odmah izvršava.
 *
 * Oblik:
 *
 * (function () {
 *     ...
 * })();
 *
 * ZAŠTO?
 *
 * Sve lokalne konstante i funkcije ostaju unutar ovog scopea
 * i ne zagađuju globalni window objekt.
 *
 * Samo funkcije koje nam stvarno trebaju globalno kasnije
 * eksplicitno stavljamo na window.
 */
(function () {


    /*
     * Zadani jezik aplikacije.
     *
     * Ako korisnik nikad nije odabrao jezik
     * ili spremljena vrijednost više nije valjana,
     * aplikacija koristi engleski.
     */
    const DEFAULT_LANGUAGE = "en";


    /*
     * Ključ pod kojim se izbor jezika sprema
     * u browserov localStorage.
     *
     * U localStorage će primjerice biti:
     *
     * appLanguage = "hr"
     */
    const STORAGE_KEY = "appLanguage";


    /*
     * ============================================================
     * DOHVAT TRENUTNOG JEZIKA
     * ============================================================
     */
    function getCurrentLanguage() {

        /*
         * localStorage.getItem(...)
         *
         * dohvaća prethodno spremljenu vrijednost iz browsera.
         *
         * Ako korisnik prije nije ništa spremio,
         * vraća null.
         */
        const saved =
            localStorage.getItem(STORAGE_KEY);


        /*
         * Provjeravamo dvije stvari:
         *
         * 1. postoji li spremljena vrijednost
         *
         * 2. postoji li taj jezik u translations objektu
         *
         * Primjer:
         *
         * saved = "hr"
         *
         * window.translations["hr"]
         *
         * postoji -> koristimo hrvatski.
         */
        if (
            saved &&
            window.translations[saved]
        ) {
            return saved;
        }


        /*
         * Ako ništa nije spremljeno ili vrijednost nije valjana,
         * vraćamo default.
         */
        return DEFAULT_LANGUAGE;
    }


    /*
     * ============================================================
     * PROMJENA JEZIKA
     * ============================================================
     *
     * Primjer poziva:
     *
     * setLanguage("hr");
     */
    function setLanguage(lang) {

        /*
         * Guard clause.
         *
         * Ako translations.js nema traženi jezik,
         * ne radimo ništa.
         *
         * Time sprječavamo npr.:
         *
         * setLanguage("de")
         *
         * ako njemački nije definiran.
         */
        if (!window.translations[lang]) {
            return;
        }


        /*
         * Spremamo izbor jezika u localStorage.
         *
         * localStorage ostaje sačuvan i nakon:
         *
         * - refresh stranice
         * - zatvaranja taba
         * - ponovnog otvaranja browsera
         *
         * sve dok korisnik/browser ne izbriše storage.
         */
        localStorage.setItem(
            STORAGE_KEY,
            lang
        );


        /*
         * Odmah ponovno prevodimo trenutno otvorenu stranicu.
         */
        applyTranslations();


        /*
         * CustomEvent je vlastiti JavaScript događaj.
         *
         * Ovdje šaljemo signal cijeloj aplikaciji:
         *
         * "Jezik se promijenio."
         *
         * Druge JS datoteke mogu slušati:
         *
         * document.addEventListener("languageChanged", ...)
         */
        document.dispatchEvent(
            new CustomEvent(
                "languageChanged",
                {

                    /*
                     * detail omogućuje da uz event pošaljemo
                     * dodatne podatke.
                     *
                     * Primjer:
                     *
                     * event.detail.language
                     *
                     * -> "hr"
                     */
                    detail: {
                        language: lang
                    }
                }
            )
        );
    }


    /*
     * ============================================================
     * FUNKCIJA t()
     * ============================================================
     *
     * t = translate
     *
     * Ovo je centralna funkcija za dohvat jednog prijevoda.
     *
     * Primjer:
     *
     * t("products.addToCart")
     *
     * za EN:
     * "Add to cart"
     *
     * za HR:
     * "Dodaj u košaricu"
     */
    function t(key) {

        /*
         * Prvo utvrdimo trenutno aktivni jezik.
         */
        const lang =
            getCurrentLanguage();


        /*
         * Ključ razbijamo prema točki.
         *
         * Primjer:
         *
         * "products.messages.notLoggedIn"
         *
         * postaje:
         *
         * [
         *   "products",
         *   "messages",
         *   "notLoggedIn"
         * ]
         */
        const parts =
            key.split(".");


        /*
         * Početna vrijednost je cijeli rječnik
         * trenutno aktivnog jezika.
         *
         * Ako je lang = "hr":
         *
         * value = window.translations.hr
         */
        let value =
            window.translations[lang];


        /*
         * Prolazimo kroz svaki dio ključa.
         *
         * Npr.:
         *
         * products
         * messages
         * notLoggedIn
         */
        for (const part of parts) {

            /*
             * Provjeravamo:
             *
             * 1. postoji li trenutna vrijednost
             * 2. sadrži li ona traženo svojstvo
             */
            if (
                value &&
                Object.prototype
                    .hasOwnProperty
                    .call(value, part)
            ) {

                /*
                 * Spuštamo se jednu razinu dublje.
                 *
                 * Primjer:
                 *
                 * value = translations.hr
                 *
                 * part = "products"
                 *
                 * nakon:
                 *
                 * value = value["products"]
                 *
                 * value sada predstavlja:
                 *
                 * translations.hr.products
                 */
                value =
                    value[part];

            } else {

                /*
                 * Ako ključ ne postoji,
                 * vraćamo sam key.
                 *
                 * To je korisno za debugging.
                 *
                 * Umjesto praznog teksta vidjeli bismo:
                 *
                 * products.someMissingTranslation
                 */
                return key;
            }
        }


        /*
         * Nakon što smo prošli cijeli path,
         * value je konačni prijevod.
         */
        return value;
    }


    /*
     * ============================================================
     * PRIMJENA PRIJEVODA NA CIJELI DOM
     * ============================================================
     */
    function applyTranslations() {

        /*
         * <html lang="...">
         *
         * Postavljamo language atribut cijelog dokumenta.
         *
         * Primjer:
         *
         * <html lang="hr">
         *
         * To je korisno za:
         *
         * - accessibility
         * - screen readere
         * - browser
         * - SEO
         */
        document.documentElement.lang =
            getCurrentLanguage();


        /*
         * ========================================================
         * PRIJEVOD OBIČNOG TEKSTA
         * ========================================================
         *
         * Tražimo sve HTML elemente koji imaju:
         *
         * data-i18n
         *
         * Primjer HTML-a:
         *
         * <button data-i18n="products.addToCart">
         *     Add to cart
         * </button>
         */
        document
            .querySelectorAll("[data-i18n]")
            .forEach(el => {

                /*
                 * Dohvaćamo vrijednost atributa.
                 *
                 * Primjer:
                 *
                 * "products.addToCart"
                 */
                const key =
                    el.getAttribute(
                        "data-i18n"
                    );


                /*
                 * t(key) dohvaća prijevod.
                 *
                 * textContent mijenja samo tekst elementa,
                 * ne interpretira ga kao HTML.
                 */
                el.textContent =
                    t(key);
            });


        /*
         * ========================================================
         * PRIJEVOD PLACEHOLDERA
         * ========================================================
         *
         * Primjer:
         *
         * <input
         *   data-i18n-placeholder="products.searchPlaceholder"
         * >
         */
        document
            .querySelectorAll(
                "[data-i18n-placeholder]"
            )
            .forEach(el => {

                const key =
                    el.getAttribute(
                        "data-i18n-placeholder"
                    );


                /*
                 * Ne mijenjamo textContent,
                 * nego HTML atribut:
                 *
                 * placeholder="..."
                 */
                el.setAttribute(
                    "placeholder",
                    t(key)
                );
            });


        /*
         * ========================================================
         * PRIJEVOD TITLE ATRIBUTA
         * ========================================================
         *
         * Primjer:
         *
         * <button
         *   title="Delete"
         *   data-i18n-title="common.delete"
         * >
         */
        document
            .querySelectorAll(
                "[data-i18n-title]"
            )
            .forEach(el => {

                const key =
                    el.getAttribute(
                        "data-i18n-title"
                    );

                el.setAttribute(
                    "title",
                    t(key)
                );
            });


        /*
         * ========================================================
         * PRIJEVOD <title> ELEMENTA
         * ========================================================
         *
         * Primjer HTML:
         *
         * <title data-i18n="profile.pageTitle">
         *     My profile
         * </title>
         */
        const titleEl =
            document.querySelector(
                "title[data-i18n]"
            );


        if (titleEl) {

            titleEl.textContent =
                t(
                    titleEl.getAttribute(
                        "data-i18n"
                    )
                );
        }


        /*
         * Posebno dohvaćamo labelu jezika.
         *
         * Ovo je samo dodatna UI pomoć.
         */
        const langLabel =
            document.getElementById(
                "languageLabel"
            );


        if (langLabel) {

            langLabel.textContent =
                t("common.language");
        }


        /*
         * Dohvaćamo trenutno aktivni jezik
         * kako bismo označili odgovarajući gumb.
         */
        const currentLang =
            getCurrentLanguage();


        const hrBtn =
            document.getElementById(
                "langHrBtn"
            );


        const enBtn =
            document.getElementById(
                "langEnBtn"
            );


        /*
         * classList.toggle(className, condition)
         *
         * Ako je condition true:
         * doda klasu.
         *
         * Ako je false:
         * ukloni klasu.
         *
         * Dakle ako je HR aktivan:
         *
         * HR gumb dobiva:
         *
         * active-lang
         */
        if (hrBtn) {

            hrBtn.classList.toggle(
                "active-lang",
                currentLang === "hr"
            );
        }


        /*
         * Isto za engleski.
         */
        if (enBtn) {

            enBtn.classList.toggle(
                "active-lang",
                currentLang === "en"
            );
        }
    }


    /*
     * ============================================================
     * IZLAGANJE FUNKCIJA GLOBALNO
     * ============================================================
     *
     * Zbog IIFE-a ove funkcije bi normalno bile privatne
     * i dostupne samo unutar ove funkcije.
     *
     * Nama ih trebaju druge JS datoteke i HTML.
     *
     * Zato ih eksplicitno stavljamo na window.
     */


    /*
     * Omogućuje:
     *
     * window.getCurrentLanguage()
     *
     * ali i jednostavno:
     *
     * getCurrentLanguage()
     */
    window.getCurrentLanguage =
        getCurrentLanguage;


    /*
     * Omogućuje drugim dijelovima aplikacije:
     *
     * setLanguage("hr");
     */
    window.setLanguage =
        setLanguage;


    /*
     * Centralna translate funkcija dostupna je svugdje:
     *
     * t("common.saveChanges")
     */
    window.t =
        t;


    /*
     * Omogućuje drugim JS datotekama da ponovno
     * prevedu trenutno otvoreni DOM.
     */
    window.applyTranslations =
        applyTranslations;

})();