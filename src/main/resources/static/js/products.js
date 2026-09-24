/*
 * ============================================================
 * PRODUCTS.JS
 * ============================================================
 *
 * Ova datoteka upravlja glavnim katalogom proizvoda.
 *
 * Glavne odgovornosti:
 * 1. dohvat proizvoda s backenda,
 * 2. dohvat postavki aplikacije,
 * 3. pretraživanje i filtriranje proizvoda,
 * 4. sortiranje proizvoda,
 * 5. paginacija,
 * 6. prikaz proizvoda u HTML-u,
 * 7. dodavanje proizvoda u košaricu,
 * 8. integracija s i18n sustavom.
 *
 * Za obranu je posebno važna zbog zahtjeva:
 * - sortiranje zapisa učitanih iz baze,
 * - filtriranje zapisa učitanih iz baze.
 */


/*
 * Svi proizvodi koje smo dobili s backenda.
 *
 * Nakon GET /products ovdje spremamo cijeli rezultat.
 *
 * Ovaj array se ne mijenja prilikom filtriranja,
 * nego predstavlja izvorni skup proizvoda.
 */
let allProducts = [];


/*
 * Proizvodi koji su prošli trenutno aktivne filtere.
 *
 * Primjer:
 *
 * allProducts:
 * [Laptop, Monitor, Mouse, Keyboard]
 *
 * search = "mouse"
 *
 * filteredProducts:
 * [Mouse]
 */
let filteredProducts = [];


/*
 * Trenutna stranica paginacije.
 *
 * Stranice počinju od 1.
 */
let currentPage = 1;


/*
 * Broj proizvoda koji se prikazuje na jednoj stranici.
 *
 * Početno je null jer se vrijednost dohvaća iz
 * app-settings.ini preko backend endpointa /settings.
 */
let itemsPerPage = null;


/*
 * DOMContentLoaded se aktivira kada browser završi
 * parsiranje HTML dokumenta.
 *
 * To znači da tada možemo sigurno koristiti:
 *
 * document.getElementById(...)
 *
 * jer HTML elementi već postoje.
 *
 * "async" omogućuje korištenje await unutar funkcije.
 */
document.addEventListener("DOMContentLoaded", async function () {

    /*
     * Funkcija iz našeg i18n sustava.
     *
     * Pronalazi elemente koji trebaju prijevod i
     * primjenjuje tekst za trenutno odabrani jezik.
     */
    applyTranslations();


    /*
     * "languageChanged" je custom event našeg i18n sustava.
     *
     * Kada korisnik promijeni jezik:
     * - ponovno prevodimo UI,
     * - ponovno generiramo category dropdown,
     * - ponovno prikazujemo proizvode.
     */
    document.addEventListener("languageChanged", () => {
        applyTranslations();
        populateCategoryFilter();
        renderProductsPage();
    });


    /*
     * Povezujemo HTML kontrole:
     *
     * search
     * category filter
     * stock filter
     * sort
     * clear filters
     *
     * s JavaScript eventima.
     */
    bindCatalogControls();


    /*
     * await zaustavlja izvršavanje ove async funkcije
     * dok Promise koji vrati loadSettings() nije riješen.
     *
     * Prvo želimo znati itemsPerPage.
     */
    await loadSettings();


    /*
     * Nakon postavki dohvaćamo proizvode.
     *
     * Redoslijed je namjeran jer renderProductsPage()
     * treba itemsPerPage za paginaciju.
     */
    await loadProducts();
});


/*
 * ============================================================
 * POVEZIVANJE UI KONTROLA
 * ============================================================
 *
 * Event listeneri povezuju korisničke akcije
 * s funkcijama za filtriranje i sortiranje.
 */
function bindCatalogControls() {

    /*
     * getElementById traži HTML element prema njegovom id atributu.
     *
     * Ako element ne postoji, rezultat je null.
     */
    const searchInput =
        document.getElementById("productSearch");

    const categoryFilter =
        document.getElementById("categoryFilter");

    const stockFilter =
        document.getElementById("stockFilter");

    const sortSelect =
        document.getElementById("sortSelect");

    const clearButton =
        document.getElementById("clearFiltersButton");


    /*
     * Provjeravamo postoji li element prije nego mu
     * pokušamo dodati event listener.
     *
     * Time izbjegavamo:
     *
     * Cannot read properties of null
     */
    if (searchInput) {

        /*
         * "input" se aktivira praktički pri svakoj
         * promjeni sadržaja input polja.
         *
         * Primjer:
         * korisnik upiše "lap" -> odmah se filtrira katalog.
         */
        searchInput.addEventListener("input", () => {

            /*
             * Nakon promjene filtera vraćamo korisnika
             * na prvu stranicu.
             *
             * Inače bi mogao ostati npr. na stranici 5,
             * a novi rezultat možda ima samo jednu stranicu.
             */
            currentPage = 1;

            /*
             * Ponovno filtriraj, sortiraj i renderiraj.
             */
            applyFiltersAndSort();
        });
    }


    if (categoryFilter) {

        /*
         * "change" event se aktivira kada korisnik
         * odabere drugu vrijednost iz <select> elementa.
         */
        categoryFilter.addEventListener("change", () => {
            currentPage = 1;
            applyFiltersAndSort();
        });
    }


    if (stockFilter) {
        stockFilter.addEventListener("change", () => {
            currentPage = 1;
            applyFiltersAndSort();
        });
    }


    if (sortSelect) {
        sortSelect.addEventListener("change", () => {
            currentPage = 1;
            applyFiltersAndSort();
        });
    }


    if (clearButton) {

        /*
         * Klik gumba poziva clearFilters().
         *
         * Ovdje možemo direktno proslijediti referencu funkcije.
         */
        clearButton.addEventListener(
            "click",
            clearFilters
        );
    }
}


/*
 * ============================================================
 * DOHVAT POSTAVKI
 * ============================================================
 *
 * Funkcija je async jer radi HTTP request.
 */
async function loadSettings() {

    try {

        /*
         * fetch() šalje HTTP request.
         *
         * Pošto method nije naveden, koristi se GET.
         */
        const response =
            await fetch(
                "http://localhost:8080/settings",
                {

                    /*
                     * credentials: "include"
                     *
                     * govori browseru da uz request pošalje
                     * credentials poput session cookieja.
                     *
                     * Naša autentikacija koristi Spring Security
                     * session, pa je ovo važno na zaštićenim endpointima.
                     */
                    credentials: "include"
                }
            );


        /*
         * response.ok je true za uspješne HTTP statuse 200-299.
         */
        if (!response.ok) {
            throw new Error(
                "Failed to load settings"
            );
        }


        /*
         * response.json():
         *
         * JSON response body pretvara u JavaScript objekt.
         *
         * Primjer odgovora:
         *
         * {
         *   language: "en",
         *   theme: "dark",
         *   itemsPerPage: 8
         * }
         */
        const settings =
            await response.json();


        /*
         * Number() pretvara vrijednost u JavaScript number.
         *
         * || 8 predstavlja fallback.
         *
         * Ako vrijednost nije valjana ili je falsy,
         * koristi se 8.
         */
        itemsPerPage =
            Number(settings.itemsPerPage) || 8;

    } catch (error) {

        /*
         * Ako fetch ili obrada odgovora ne uspije,
         * grešku ispisujemo u developer console.
         */
        console.error(
            "Settings load error:",
            error
        );


        /*
         * Aplikacija i dalje može raditi koristeći
         * default vrijednost.
         */
        itemsPerPage = 8;
    }
}


/*
 * ============================================================
 * DOHVAT PROIZVODA
 * ============================================================
 *
 * OVO JE VAŽNO ZA ZAHTJEV SORTIRANJA/FILTRIRANJA.
 *
 * Proizvodi koji će se kasnije sortirati i filtrirati
 * prvo se dohvaćaju s backenda.
 */
async function loadProducts() {

    /*
     * #main je HTML container u koji renderiramo kartice proizvoda.
     */
    const container =
        document.getElementById("main");

    try {

        /*
         * HTTP GET:
         *
         * GET http://localhost:8080/products
         *
         * Backend preko ProductService/ProductRepository
         * dohvaća proizvode iz baze.
         */
        const response =
            await fetch(
                "http://localhost:8080/products"
            );


        if (!response.ok) {

            /*
             * Template literal koristi ` `.
             *
             * ${response.status} umeće vrijednost varijable
             * direktno u String.
             */
            throw new Error(
                `Failed to fetch products (${response.status})`
            );
        }


        /*
         * JSON array s backenda pretvaramo u
         * JavaScript array.
         *
         * allProducts sada predstavlja proizvode
         * učitane iz baze.
         */
        allProducts =
            await response.json();


        /*
         * Na temelju učitanih proizvoda generiramo
         * popis dostupnih kategorija.
         */
        populateCategoryFilter();


        /*
         * Zatim primjenjujemo trenutne filtere/sort
         * i prikazujemo rezultat.
         */
        applyFiltersAndSort();

    } catch (error) {

        console.error(
            "Error while fetching products:",
            error
        );


        /*
         * innerHTML mijenja HTML sadržaj elementa.
         *
         * t(...) vraća prijevod odgovarajućeg ključa.
         */
        container.innerHTML =
            `<p class="empty-message">${t("products.loadError")}</p>`;
    }
}


/*
 * ============================================================
 * GENERIRANJE CATEGORY FILTERA
 * ============================================================
 */
function populateCategoryFilter() {

    const categoryFilter =
        document.getElementById("categoryFilter");


    /*
     * Guard clause.
     *
     * Ako element ne postoji, odmah prekidamo funkciju.
     */
    if (!categoryFilter) return;


    /*
     * Pamtimo trenutno odabranu kategoriju.
     *
     * To je korisno ako ponovno generiramo dropdown,
     * npr. nakon promjene jezika.
     */
    const currentValue =
        categoryFilter.value;


    /*
     * Ovdje se događa nekoliko operacija.
     *
     * 1. allProducts.map(...)
     *
     * map() svaki Product pretvara u naziv njegove kategorije.
     *
     * Product -> String
     *
     *
     * 2. product.category?.name
     *
     * ?. je OPTIONAL CHAINING.
     *
     * Ako category postoji:
     *     vrati category.name
     *
     * Ako je category null/undefined:
     *     vrati undefined
     *
     * Time izbjegavamo:
     * Cannot read properties of null
     *
     *
     * 3. .filter(Boolean)
     *
     * uklanja falsy vrijednosti poput:
     *
     * null
     * undefined
     * ""
     *
     *
     * 4. new Set(...)
     *
     * Set ne dopušta duplikate.
     *
     * Ako imamo:
     *
     * Laptop -> Computers
     * PC     -> Computers
     * Mouse  -> Peripherals
     *
     * rezultat je:
     *
     * Computers
     * Peripherals
     *
     *
     * 5. [...new Set(...)]
     *
     * Spread operator ... pretvara Set ponovno u Array.
     *
     *
     * 6. sort(...)
     *
     * abecedno sortira nazive kategorija.
     */
    const categories = [...new Set(
        allProducts
            .map(product =>
                product.category?.name
            )
            .filter(Boolean)
    )].sort(
        (a, b) =>
            a.localeCompare(b)
    );


    /*
     * Prvo kreiramo opciju "All categories".
     */
    categoryFilter.innerHTML = `
        <option value="">
            ${t("products.allCategories")}
        </option>
    `;


    /*
     * Za svaku pronađenu kategoriju stvaramo
     * novi HTML <option>.
     */
    categories.forEach(categoryName => {

        /*
         * createElement dinamički stvara HTML element.
         */
        const option =
            document.createElement("option");

        /*
         * value je vrijednost koja će biti dostupna preko:
         *
         * categoryFilter.value
         */
        option.value =
            categoryName;

        /*
         * textContent određuje tekst koji korisnik vidi.
         */
        option.textContent =
            categoryName;

        /*
         * Dodajemo <option> u <select>.
         */
        categoryFilter.appendChild(option);
    });


    /*
     * Vraćamo prethodno odabranu vrijednost.
     */
    categoryFilter.value =
        currentValue;
}


/*
 * ============================================================
 * FILTRIRANJE I SORTIRANJE
 * ============================================================
 *
 * OBRANA:
 *
 * Ova metoda je jedan od glavnih dokaza za zahtjev:
 *
 * "Aplikacija ima mogućnost filtriranja zapisa
 * učitanih iz baze podataka."
 */
function applyFiltersAndSort() {

    /*
     * ?.value je optional chaining.
     *
     * Ako element ne postoji -> undefined.
     *
     * || "" tada daje prazan String.
     *
     * toLowerCase() omogućuje case-insensitive search.
     *
     * trim() uklanja razmake s početka i kraja.
     */
    const searchValue =
        (
            document
                .getElementById("productSearch")
                ?.value || ""
        )
            .toLowerCase()
            .trim();


    /*
     * Trenutno odabrana kategorija.
     */
    const categoryValue =
        document
            .getElementById("categoryFilter")
            ?.value || "";


    /*
     * Trenutno odabrani stock filter.
     */
    const stockValue =
        document
            .getElementById("stockFilter")
            ?.value || "";


    /*
     * Trenutno odabrani način sortiranja.
     *
     * Ako nije pronađen -> "default".
     */
    const sortValue =
        document
            .getElementById("sortSelect")
            ?.value || "default";


    /*
     * Array.filter() NE mijenja originalni allProducts.
     *
     * Vraća novi array koji sadrži samo elemente
     * za koje callback funkcija vrati true.
     */
    filteredProducts =
        allProducts.filter(product => {


            /*
             * Kreiramo jedan String koji sadrži više
             * atributa proizvoda.
             *
             * Zato jedan search može pretraživati:
             *
             * - name
             * - brand
             * - description
             * - category
             */
            const searchableText = [
                product.name,
                product.brand,
                product.description,
                product.category?.name
            ]
                /*
                 * join(" ") spaja elemente arraya u String,
                 * odvojene razmakom.
                 */
                .join(" ")

                /*
                 * Pretvaramo sve u lowercase kako bi
                 * pretraga bila case-insensitive.
                 */
                .toLowerCase();


            /*
             * SEARCH FILTER.
             *
             * !searchValue:
             * ako korisnik nije ništa upisao -> true.
             *
             * includes(searchValue):
             * provjerava sadrži li tekst traženi substring.
             */
            const matchesSearch =
                !searchValue ||
                searchableText.includes(
                    searchValue
                );


            /*
             * CATEGORY FILTER.
             *
             * Ako kategorija nije odabrana:
             * svi proizvodi prolaze.
             *
             * Inače naziv kategorije proizvoda mora
             * odgovarati odabranoj vrijednosti.
             */
            const matchesCategory =
                !categoryValue ||
                product.category?.name ===
                categoryValue;


            /*
             * STOCK FILTER.
             *
             * "" -> bez stock filtera
             *
             * inStock -> stock > 0
             *
             * outOfStock -> stock <= 0
             */
            const matchesStock =
                stockValue === "" ||

                (
                    stockValue === "inStock" &&
                    product.stock > 0
                ) ||

                (
                    stockValue === "outOfStock" &&
                    product.stock <= 0
                );


            /*
             * Proizvod ostaje u filteredProducts samo ako
             * zadovoljava SVA TRI uvjeta.
             *
             * && predstavlja logički AND.
             */
            return matchesSearch &&
                matchesCategory &&
                matchesStock;
        });


    /*
     * Nakon filtriranja sortiramo dobiveni rezultat.
     */
    sortProducts(sortValue);


    /*
     * Nakon toga ponovno crtamo katalog.
     */
    renderProductsPage();
}


/*
 * ============================================================
 * SORTIRANJE
 * ============================================================
 *
 * OBRANA:
 *
 * Ovo pokriva zahtjev:
 *
 * "Aplikacija ima mogućnost sortiranja zapisa
 * učitanih iz baze podataka."
 *
 * Array.sort() sortira elemente arraya.
 *
 * Comparator prima dva elementa:
 *
 * a i b
 *
 * i vraća:
 *
 * < 0 -> a prije b
 * > 0 -> b prije a
 * = 0 -> smatraju se jednakima za sortiranje
 */
function sortProducts(sortValue) {

    switch (sortValue) {

        /*
         * CIJENA: najmanja -> najveća.
         *
         * Number() osigurava numeričku usporedbu.
         */
        case "priceAsc":

            filteredProducts.sort(
                (a, b) =>
                    Number(a.price) -
                    Number(b.price)
            );

            break;


        /*
         * CIJENA: najveća -> najmanja.
         */
        case "priceDesc":

            filteredProducts.sort(
                (a, b) =>
                    Number(b.price) -
                    Number(a.price)
            );

            break;


        /*
         * NAZIV: A -> Z.
         *
         * localeCompare() uspoređuje Stringove
         * prema pravilima tekstualnog sortiranja.
         */
        case "nameAsc":

            filteredProducts.sort(
                (a, b) =>
                    a.name.localeCompare(
                        b.name
                    )
            );

            break;


        /*
         * NAZIV: Z -> A.
         */
        case "nameDesc":

            filteredProducts.sort(
                (a, b) =>
                    b.name.localeCompare(
                        a.name
                    )
            );

            break;


        /*
         * STOCK: najveći -> najmanji.
         */
        case "stockDesc":

            filteredProducts.sort(
                (a, b) =>
                    Number(b.stock) -
                    Number(a.stock)
            );

            break;


        /*
         * Ako korisnik nije odabrao poseban sort,
         * proizvode sortiramo prema ID-u uzlazno.
         */
        default:

            filteredProducts.sort(
                (a, b) =>
                    Number(a.id) -
                    Number(b.id)
            );

            break;
    }
}


/*
 * ============================================================
 * RENDERIRANJE TRENUTNE STRANICE PROIZVODA
 * ============================================================
 */
function renderProductsPage() {

    const container =
        document.getElementById("main");


    /*
     * Brišemo prethodno prikazane kartice.
     */
    container.innerHTML = "";


    /*
     * Ako nakon filtriranja nema rezultata,
     * prikazujemo odgovarajuću poruku.
     */
    if (
        !filteredProducts ||
        filteredProducts.length === 0
    ) {

        container.innerHTML =
            `<p class="empty-message">
                ${t("products.noMatchingProducts")}
             </p>`;


        /*
         * Ažuriramo tekst s informacijama o rezultatima.
         */
        updateResultInfo();


        /*
         * Paginacija će se također prilagoditi
         * praznom rezultatu.
         */
        renderPagination();

        return;
    }


    /*
     * Izračun ukupnog broja stranica.
     *
     * Primjer:
     *
     * 17 proizvoda
     * 8 po stranici
     *
     * 17 / 8 = 2.125
     *
     * Math.ceil(2.125) = 3 stranice.
     */
    const totalPages =
        Math.ceil(
            filteredProducts.length /
            itemsPerPage
        );


    /*
     * Defensive check.
     *
     * Ako je currentPage nakon filtriranja veći
     * od ukupnog broja stranica, vraćamo ga na
     * posljednju postojeću stranicu.
     */
    if (currentPage > totalPages) {
        currentPage = totalPages;
    }


    /*
     * Računamo početni index.
     *
     * Ako smo na stranici 2 i imamo 8 proizvoda po stranici:
     *
     * (2 - 1) * 8 = 8
     *
     * Dakle krećemo od indexa 8.
     */
    const startIndex =
        (currentPage - 1) *
        itemsPerPage;


    /*
     * Gornja granica slice operacije.
     */
    const endIndex =
        startIndex +
        itemsPerPage;


    /*
     * slice(start, end)
     *
     * uzima samo dio arraya potreban za trenutnu stranicu.
     *
     * Ne mijenja originalni array.
     */
    const productsForPage =
        filteredProducts.slice(
            startIndex,
            endIndex
        );


    /*
     * forEach prolazi kroz svaki proizvod
     * trenutne stranice.
     */
    productsForPage.forEach(product => {

        /*
         * Dinamički stvaramo <div>.
         */
        const productCard =
            document.createElement("div");


        /*
         * Dodajemo CSS klasu.
         */
        productCard.classList.add(
            "product-card"
        );


        /*
         * Ternary operator:
         *
         * condition ? vrijednostAkoTrue : vrijednostAkoFalse
         *
         * Ako proizvod ima kategoriju, koristimo njezin naziv.
         * Inače koristimo prevedeni fallback tekst.
         */
        const categoryName =
            product.category
                ? product.category.name
                : t("products.unknownCategory");


        /*
         * Određivanje URL-a slike.
         *
         * hasImage dolazi s backend reprezentacije proizvoda.
         *
         * Ako proizvod ima sliku:
         * GET /products/{id}/image
         *
         * Inače koristimo placeholder.
         */
        const imageSrc =
            product.hasImage
                ? `http://localhost:8080/products/${product.id}/image`
                : "/images/product-placeholder.png";


        /*
         * Ako description postoji, prikazujemo ga.
         * Inače prikazujemo prevedeni fallback.
         */
        const description =
            product.description
                ? product.description
                : t("products.noDescription");


        /*
         * Template literal koristimo za generiranje
         * HTML-a kartice proizvoda.
         *
         * ${...} umeće JavaScript vrijednost u String.
         */
        productCard.innerHTML = `
            <div class="product-image-wrapper">

                <img
                    class="product-image"
                    src="${imageSrc}"
                    alt="${product.name}"
                >

            </div>

            <div class="product-content">

                <h3 class="product-title">
                    ${product.name}
                </h3>

                <p class="product-brand">

                    <strong>
                        ${t("products.brand")}:
                    </strong>

                    ${product.brand}

                </p>

                <p class="product-category">

                    <strong>
                        ${t("products.category")}:
                    </strong>

                    ${categoryName}

                </p>

                <p class="product-description">
                    ${description}
                </p>

                <div class="product-meta">

                    <!--
                        Number(...).toFixed(2)

                        osigurava prikaz cijene na dvije decimale.
                        Primjer: 10 -> 10.00
                    -->
                    <span class="product-price">
                        ${Number(product.price).toFixed(2)} EUR
                    </span>

                    <!--
                        Dinamički biramo CSS klasu prema stocku.

                        stock > 0 -> in-stock
                        inače -> out-of-stock
                    -->
                    <span class="
                        product-stock
                        ${product.stock > 0
            ? "in-stock"
            : "out-of-stock"}
                    ">

                        ${
            product.stock > 0

                /*
                 * Ako proizvod postoji na zalihi,
                 * prikazujemo i količinu.
                 */
                ? `${t("products.inStock")}: ${product.stock}`

                /*
                 * Inače samo "Out of stock".
                 */
                : t("products.outOfStock")
        }

                    </span>

                </div>


                <!--
                    Ako nema zalihe, dodajemo disabled atribut
                    i korisnik ne može kliknuti Add to cart.
                -->
                <button
                    class="add-to-cart-btn"
                    ${product.stock <= 0
            ? "disabled"
            : ""}
                >
                    ${t("products.addToCart")}
                </button>

            </div>
        `;


        /*
         * querySelector traži element UNUTAR productCarda
         * prema CSS selektoru.
         */
        const button =
            productCard.querySelector(
                ".add-to-cart-btn"
            );


        /*
         * Kada korisnik klikne gumb,
         * šaljemo upravo taj Product objekt funkciji addToCart().
         *
         * Arrow funkcija omogućuje da product ostane dostupan
         * kroz closure.
         */
        button.addEventListener(
            "click",
            () => addToCart(product)
        );


        /*
         * Završenu karticu dodajemo u #main container.
         */
        container.appendChild(
            productCard
        );
    });


    /*
     * Nakon renderiranja proizvoda ažuriramo
     * pomoćne UI elemente.
     */
    updateResultInfo();
    renderPagination();
}


/*
 * ============================================================
 * INFORMACIJA O TRENUTNO PRIKAZANIM REZULTATIMA
 * ============================================================
 *
 * Primjer:
 *
 * Showing 9-16 of 25
 */
function updateResultInfo() {

    const info =
        document.getElementById(
            "productResultInfo"
        );


    /*
     * Ako HTML element ne postoji,
     * nema što ažurirati.
     */
    if (!info) return;


    /*
     * Poseban tekst ako nema rezultata.
     */
    if (
        !filteredProducts ||
        filteredProducts.length === 0
    ) {

        info.textContent =
            t("products.noResultsInfo");

        return;
    }


    /*
     * Prvi prikazani redni broj.
     *
     * Ovdje računamo za korisnički prikaz koji počinje od 1,
     * dok JavaScript array index počinje od 0.
     */
    const start =
        (currentPage - 1) *
        itemsPerPage + 1;


    /*
     * Math.min() vraća manju od dvije vrijednosti.
     *
     * Potrebno je zbog zadnje stranice.
     *
     * Npr. imamo 18 proizvoda i 8 po stranici:
     *
     * stranica 3 matematički završava na 24,
     * ali stvarno postoji samo 18 proizvoda.
     *
     * Math.min(24, 18) = 18.
     */
    const end =
        Math.min(
            currentPage * itemsPerPage,
            filteredProducts.length
        );


    /*
     * Prikaz:
     *
     * Showing 1-8 of 25
     */
    info.textContent =
        `${t("products.showing")} ` +
        `${start}-${end} ` +
        `${t("products.of")} ` +
        `${filteredProducts.length}`;
}


/*
 * ============================================================
 * PAGINACIJA
 * ============================================================
 */
function renderPagination() {

    const pagination =
        document.getElementById(
            "pagination"
        );

    if (!pagination) return;


    /*
     * Brišemo prethodno generirane gumbe.
     */
    pagination.innerHTML = "";


    /*
     * Ako svi rezultati stanu na jednu stranicu,
     * paginacija nije potrebna.
     */
    if (
        !filteredProducts ||
        filteredProducts.length <= itemsPerPage
    ) {
        return;
    }


    /*
     * Izračun broja stranica.
     */
    const totalPages =
        Math.ceil(
            filteredProducts.length /
            itemsPerPage
        );


    /*
     * PREVIOUS BUTTON.
     */
    const previousButton =
        document.createElement("button");

    previousButton.textContent = "‹";


    /*
     * Na prvoj stranici Previous mora biti disabled.
     */
    previousButton.disabled =
        currentPage === 1;


    /*
     * onclick je drugi način registriranja click handlera.
     *
     * Ovdje idemo jednu stranicu unatrag.
     */
    previousButton.onclick =
        () => goToPage(
            currentPage - 1
        );


    pagination.appendChild(
        previousButton
    );


    /*
     * Generiramo gumb za svaku stranicu.
     *
     * Ako imamo 4 stranice:
     *
     * 1 2 3 4
     */
    for (
        let page = 1;
        page <= totalPages;
        page++
    ) {

        const pageButton =
            document.createElement("button");

        pageButton.textContent =
            page;


        /*
         * classList.toggle(className, condition)
         *
         * dodaje "active-page" ako je condition true,
         * a uklanja ako je false.
         *
         * Tako CSS može označiti trenutnu stranicu.
         */
        pageButton.classList.toggle(
            "active-page",
            page === currentPage
        );


        /*
         * Closure pamti vrijednost page
         * za odgovarajući button.
         */
        pageButton.onclick =
            () => goToPage(page);


        pagination.appendChild(
            pageButton
        );
    }


    /*
     * NEXT BUTTON.
     */
    const nextButton =
        document.createElement("button");

    nextButton.textContent = "›";


    /*
     * Na zadnjoj stranici Next je disabled.
     */
    nextButton.disabled =
        currentPage === totalPages;


    nextButton.onclick =
        () => goToPage(
            currentPage + 1
        );


    pagination.appendChild(
        nextButton
    );
}


/*
 * ============================================================
 * PROMJENA STRANICE
 * ============================================================
 */
function goToPage(page) {

    /*
     * Ponovno računamo broj stranica kako bismo
     * provjerili je li tražena stranica validna.
     */
    const totalPages =
        Math.ceil(
            filteredProducts.length /
            itemsPerPage
        );


    /*
     * Guard clause:
     *
     * ne dopuštamo:
     *
     * page < 1
     *
     * niti
     *
     * page > totalPages
     */
    if (
        page < 1 ||
        page > totalPages
    ) {
        return;
    }


    /*
     * Postavljamo novu trenutnu stranicu.
     */
    currentPage = page;


    /*
     * Ponovno renderiramo proizvode.
     *
     * NEMA novog HTTP requesta.
     *
     * Proizvodi su već u filteredProducts.
     */
    renderProductsPage();


    /*
     * Vraćamo korisnika na vrh stranice.
     *
     * behavior: "smooth"
     * daje animirano pomicanje.
     */
    window.scrollTo({
        top: 0,
        behavior: "smooth"
    });
}


/*
 * ============================================================
 * RESET FILTERA
 * ============================================================
 */
function clearFilters() {

    const searchInput =
        document.getElementById(
            "productSearch"
        );

    const categoryFilter =
        document.getElementById(
            "categoryFilter"
        );

    const stockFilter =
        document.getElementById(
            "stockFilter"
        );

    const sortSelect =
        document.getElementById(
            "sortSelect"
        );


    /*
     * Svaku kontrolu vraćamo na početnu vrijednost.
     *
     * Prvo provjeravamo postoji li element.
     */
    if (searchInput) {
        searchInput.value = "";
    }

    if (categoryFilter) {
        categoryFilter.value = "";
    }

    if (stockFilter) {
        stockFilter.value = "";
    }

    if (sortSelect) {
        sortSelect.value = "default";
    }


    /*
     * Nakon resetiranja krećemo ponovno od stranice 1.
     */
    currentPage = 1;


    /*
     * Ponovno izračunavamo prikaz.
     */
    applyFiltersAndSort();
}


/*
 * ============================================================
 * DODAVANJE PROIZVODA U KOŠARICU
 * ============================================================
 *
 * Funkcija prima Product objekt koji je korisnik odabrao.
 */
async function addToCart(product) {

    try {

        /*
         * Prvo provjeravamo postoji li trenutno
         * autentificirani korisnik.
         *
         * /auth/me na backendu koristi Spring Security
         * Authentication/session.
         */
        const meResponse =
            await fetch(
                "http://localhost:8080/auth/me",
                {

                    /*
                     * Šaljemo session cookie.
                     */
                    credentials: "include"
                }
            );


        /*
         * Ako /auth/me nije uspješan,
         * korisnik nije prijavljen.
         */
        if (!meResponse.ok) {

            /*
             * alert() prikazuje browser popup.
             */
            alert(
                t("products.messages.notLoggedIn")
            );


            /*
             * Preusmjeravanje na login stranicu.
             */
            window.location.href =
                "/html/signIn.html";

            return;
        }


        /*
         * Korisnik je prijavljen.
         *
         * Šaljemo zahtjev backendu za dodavanje
         * jednog komada proizvoda u njegovu košaricu.
         */
        const response =
            await fetch(
                "http://localhost:8080/cart/items",
                {

                    /*
                     * POST jer stvaramo/dodajemo resurs.
                     */
                    method: "POST",


                    /*
                     * Backend mora znati da je request body JSON.
                     */
                    headers: {
                        "Content-Type":
                            "application/json"
                    },


                    /*
                     * Uz request šaljemo session cookie,
                     * kako bi Spring Security znao
                     * koji korisnik šalje zahtjev.
                     */
                    credentials: "include",


                    /*
                     * JSON.stringify pretvara JavaScript objekt:
                     *
                     * {
                     *   productId: ...,
                     *   quantity: 1
                     * }
                     *
                     * u JSON String koji se šalje kroz HTTP body.
                     */
                    body: JSON.stringify({
                        productId: product.id,
                        quantity: 1
                    })
                }
            );


        /*
         * Ako je backend vratio uspješan 2xx status.
         */
        if (response.ok) {

            alert(
                t("products.messages.addedToCart")
            );


            /*
             * Nakon uspješnog dodavanja korisnika
             * preusmjeravamo na cart.html.
             */
            window.location.href =
                "/html/cart.html";

            return;
        }


        /*
         * HTTP 409 = Conflict.
         *
         * Aplikacija ovdje ima poseban UI odgovor
         * za konflikt prilikom dodavanja u košaricu.
         */
        if (response.status === 409) {

            /*
             * response.text() čita response body kao String.
             */
            const text =
                await response.text();


            /*
             * Ako backend nije poslao tekst,
             * koristimo prevedenu fallback poruku.
             */
            alert(
                text ||
                t(
                    "products.messages.alreadyInCart"
                )
            );

            return;
        }


        /*
         * Ostale HTTP greške.
         */
        const errorText =
            await response.text();


        alert(
            errorText ||
            t(
                "products.messages.genericError"
            )
        );

    } catch (error) {

        /*
         * Ovdje završavaju npr.:
         *
         * network error,
         * nedostupan backend,
         * neočekivana JS greška.
         */
        console.error(
            "Error adding product to cart:",
            error
        );


        /*
         * Korisniku ne prikazujemo tehničke detalje,
         * nego generičku prevedenu poruku.
         */
        alert(
            t(
                "products.messages.genericError"
            )
        );
    }
}