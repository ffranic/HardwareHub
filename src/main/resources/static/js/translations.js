window.translations = {
    en: {
        common: {
            language: "Language",
            english: "EN",
            croatian: "HR",
            edit: "Edit",
            delete: "Delete",
            cancel: "Cancel",
            saveChanges: "Save changes",
            noData: "No data.",
            loading: "Loading...",
            actions: "Actions",
            yes: "Yes",
            no: "No",
            total: "Total",
            price: "Price",
            quantity: "Quantity",
            email: "Email",
            username: "Username",
            password: "Password",
            role: "Role",
            footer: "© HardwareHub 2026 All rights reserved"
        },

        nav: {
            home: "Home",
            registration: "Registration",
            signIn: "Sign in",
            myProfile: "My profile",
            myOrders: "My orders",
            myCart: "My cart",
            administration: "Administration",
            contact: "Contact",
            logOut: "Log out"
        },

        admin: {
            pageTitle: "Administration",
            panelTitle: "Administration Panel",

            loadInventorySnapshot: "View inventory snapshot",
            verifyInventorySnapshotIntegrity: "Verify snapshot integrity",

            externalRestTitle: "External REST service",
            externalRestDescription: "Fetch the latest EUR to USD exchange rate from an external REST API.",
            loadExchangeRate: "Load EUR/USD rate",

            settingsManagement: "Application settings",
            settingsLanguage: "Default language",
            settingsTheme: "Theme",
            settingsItemsPerPage: "Products per page",
            themeDark: "Dark",
            themeLight: "Light",
            saveSettings: "Save settings",

            loadRecentActivityLogs: "Check recent activity",
            loadArchivedActivityLogs: "Check archived activity",
            verifyAuditIntegrity: "Verify audit integrity",

            userManagement: "User management",
            searchUsersPlaceholder: "Search users by username or email",
            allRoles: "All roles",
            adminRole: "Admin",
            customerRole: "Customer",

            orderManagement: "Order management",

            categoryManagement: "Category management",
            categoryNamePlaceholder: "Category name",
            addCategory: "Add category",
            cancelCategoryEdit: "Cancel edit",

            createSecureLogArchive: "Create secure archive",
            decryptSecureLogArchive: "Decrypt secure archive",

            productManagement: "Product management",
            productNamePlaceholder: "Product name",
            productBrandPlaceholder: "Brand",
            selectCategory: "Select category",
            productPricePlaceholder: "Price",
            productStockPlaceholder: "Stock",
            productDescriptionPlaceholder: "Description",
            addProduct: "Add product",
            cancelProductEdit: "Cancel edit",

            pdfBatchTitle: "Order confirmations",
            pdfBatchDescription: "Generate PDF confirmations for customer orders and store them as a ZIP archive.",
            generatePdfBatch: "Generate confirmations",
            benchmarkPdfBatch: "Run performance analysis",

            systemToolsTitle: "System maintenance & security",
            systemToolsDescription: "Tools for maintaining inventory files, securing activity logs, checking file integrity and running system analysis.",

            activityLogsTitle: "Activity logs",
            activityLogsDescription: "Review security-relevant application activity stored in JSON format.",
            loadActivityLogs: "Load activity logs",
            hashActivityLogs: "Hash logs",
            encryptActivityLogs: "Encrypt logs",
            decryptActivityLogs: "Decrypt logs",

            stockXmlTitle: "Stock XML",
            stockXmlDescription: "Export and inspect product stock snapshots stored as XML.",
            exportStockXml: "Export stock XML",
            loadStockXml: "Load stock XML",
            hashStockXml: "Hash stock XML",

            analysisTitle: "System analysis",
            analysisDescription: "Run parallel product and order analysis using a thread pool.",
            runAnalysis: "Run analysis",
            loadAnalysisResults: "Load analysis results",

            binaryBackupTitle: "Binary stock backup",
            binaryBackupDescription: "Create and read a custom binary stock backup file.",
            writeBinaryBackup: "Write binary backup",
            readBinaryBackup: "Read binary backup",

            toolOutputTitle: "Output",

            userTable: {
                id: "ID",
                username: "Username",
                email: "Email",
                role: "Role",
                changeRole: "Change role",
                actions: "Actions",
                noUsers: "No users found."
            },

            orderTable: {
                id: "ID",
                user: "User",
                email: "Email",
                date: "Date",
                status: "Status",
                total: "Total",
                changeStatus: "Change status",
                noOrders: "No orders found."
            },

            categoryTable: {
                id: "ID",
                name: "Name",
                actions: "Actions",
                noCategories: "No categories found."
            },

            productTable: {
                id: "ID",
                name: "Name",
                brand: "Brand",
                category: "Category",
                price: "Price",
                stock: "Stock",
                image: "Image",
                actions: "Actions",
                noProducts: "No products found.",
                noImage: "No image"
            },

            messages: {
                accessDenied: "Access denied.",
                notAuthenticated: "You are not logged in.",
                notAuthorized: "You are not authorized to access this page.",

                systemToolError: "System maintenance action failed.",

                settingsSaved: "Settings saved successfully.",
                settingsLoadError: "Error loading settings.",
                settingsSaveError: "Error saving settings.",

                confirmDeleteUser: "Are you sure you want to delete this user?",
                userRoleUpdated: "User role updated.",
                userDeleted: "User deleted successfully.",
                userRoleUpdateError: "Error updating user role.",
                userDeleteError: "Error deleting user.",

                orderStatusUpdated: "Order status updated.",
                orderStatusUpdateError: "Error updating order status.",

                categoryNameRequired: "Category name is required.",
                categoryNotFound: "Category not found.",
                confirmDeleteCategory: "Are you sure you want to delete this category?",
                categoryAdded: "Category added successfully.",
                categoryUpdated: "Category updated successfully.",
                categoryDeleted: "Category deleted successfully.",
                categorySaveError: "Error saving category.",
                categoryDeleteError: "Error deleting category.",

                productNotFound: "Product not found.",
                productRequiredFields: "Please fill in all required product fields.",
                confirmDeleteProduct: "Are you sure you want to delete this product?",
                productAdded: "Product added successfully.",
                productUpdated: "Product updated successfully.",
                productDeleted: "Product deleted successfully.",
                productSaveError: "Error saving product.",
                productDeleteError: "Error deleting product."
            },

            statuses: {
                NEW: "New",
                PROCESSING: "Processing",
                SHIPPED: "Shipped",
                DELIVERED: "Delivered",
                CANCELLED: "Cancelled"
            }
        },

        products: {
            heading: "Our products",
            brand: "Brand",
            category: "Category",
            addToCart: "Add to cart",
            inStock: "In stock",
            outOfStock: "Out of stock",
            noProducts: "No products available at the moment.",
            loadError: "An error occurred while loading products.",
            unknownCategory: "Unknown category",
            noDescription: "No description available.",

            searchPlaceholder: "Search products...",
            allCategories: "All categories",
            allStock: "All stock",
            inStockOnly: "In stock",
            outOfStockOnly: "Out of stock",
            sortDefault: "Default sorting",
            sortPriceAsc: "Price: low to high",
            sortPriceDesc: "Price: high to low",
            sortNameAsc: "Name: A-Z",
            sortNameDesc: "Name: Z-A",
            sortStockDesc: "Stock: high to low",
            clearFilters: "Clear filters",
            noMatchingProducts: "No products match your filters.",
            showing: "Showing",
            of: "of",
            noResultsInfo: "No products found",

            messages: {
                notLoggedIn: "You must be signed in to add products to cart.",
                addedToCart: "Product added to cart!",
                alreadyInCart: "Product is already in cart.",
                genericError: "An error occurred. Please try again."
            }
        },

        profile: {
            pageTitle: "My profile",
            heading: "My profile",
            emailLabel: "Email:",
            usernameLabel: "Username:",
            roleLabel: "Role:",
            updateProfileButton: "Update profile",

            appearanceTitle: "Appearance",
            appearanceDescription: "Choose how the application looks on this device.",
            lightTheme: "Light",
            darkTheme: "Dark",

            updateModalTitle: "Update your profile",
            saveChanges: "Save changes",

            changePasswordTitle: "Change password",
            newPasswordLabel: "New password:",
            confirmPasswordLabel: "Confirm password:",
            changePasswordButton: "Change password",

            myOrdersTitle: "My orders",
            loadingOrders: "Loading orders...",
            noOrders: "You have no orders yet.",
            deleteAccountButton: "Delete account",

            order: {
                orderId: "Order ID",
                date: "Date",
                status: "Status",
                total: "Total",
                downloadPdf: "Download PDF",
                items: "Items",
                product: "Product",
                quantity: "Quantity",
                unitPrice: "Unit price",
                subtotal: "Subtotal"
            },

            messages: {
                notLoggedIn: "You are not logged in.",
                profileUpdated: "Profile updated successfully.",
                profileUpdateError: "Error updating profile.",
                passwordsDoNotMatch: "Passwords do not match.",
                passwordRequired: "Password is required.",
                passwordUpdated: "Password updated successfully.",
                passwordUpdateError: "Error updating password.",
                confirmDeleteAccount: "Are you sure you want to delete your account?",
                accountDeleted: "Account deleted successfully.",
                accountDeleteError: "Error deleting account.",
                ordersLoadError: "Error loading orders.",
                themeSaved: "Theme preference saved."
            }
        },

        cart: {
            pageTitle: "My cart",
            heading: "My cart",
            totalPrice: "Total price",
            checkout: "Proceed to checkout",
            continueShopping: "Continue shopping",
            emptyCart: "Your cart is empty.",
            remove: "Remove",
            updateQuantity: "Update quantity",
            product: "Product",
            price: "Price",
            quantity: "Quantity",
            subtotal: "Subtotal",

            messages: {
                notLoggedIn: "You must sign in to view your cart.",
                loadError: "Error loading cart.",
                confirmRemove: "Remove this item from cart?",
                removeError: "Error removing item from cart.",
                quantityUpdateError: "Error updating quantity.",
                checkoutSuccess: "Checkout completed successfully.",
                checkoutError: "Error during checkout.",
                emptyCartCheckout: "Your cart is empty."
            }
        },

        contact: {
            pageTitle: "Contact",
            locationTitle: "Location",
            contactTitle: "Contact us",
            phoneNumber: "Phone number:",
            email: "E-mail:"
        },

        registration: {
            pageTitle: "Registration",
            heading: "Create an account",
            emailLabel: "Email:",
            usernameLabel: "Username:",
            usernamePlaceholder: "Choose username",
            passwordLabel: "Password:",
            registerButton: "Register",

            messages: {
                success: "Successful registration.",
                error: "Registration failed.",
                emailRequired: "Email is required.",
                usernameRequired: "Username is required.",
                passwordRequired: "Password is required."
            }
        },

        signIn: {
            pageTitle: "Sign in",
            heading: "Sign in",
            usernameLabel: "Username:",
            passwordLabel: "Password:",
            signInButton: "Sign in",
            noAccountText: "Don't have an account?",
            registerText: "Register",
            hereText: "here.",

            messages: {
                success: "Signed in successfully.",
                error: "Invalid username or password.",
                usernameRequired: "Username is required.",
                passwordRequired: "Password is required."
            }
        }
    },

    hr: {
        common: {
            language: "Jezik",
            english: "EN",
            croatian: "HR",
            edit: "Uredi",
            delete: "Obriši",
            cancel: "Odustani",
            saveChanges: "Spremi promjene",
            noData: "Nema podataka.",
            loading: "Učitavanje...",
            actions: "Akcije",
            yes: "Da",
            no: "Ne",
            total: "Ukupno",
            price: "Cijena",
            quantity: "Količina",
            email: "Email",
            username: "Korisničko ime",
            password: "Lozinka",
            role: "Uloga",
            footer: "© HardwareHub 2026 Sva prava pridržana"
        },

        nav: {
            home: "Početna",
            registration: "Registracija",
            signIn: "Prijava",
            myProfile: "Moj profil",
            myOrders: "Moje narudžbe",
            myCart: "Moja košarica",
            administration: "Administracija",
            contact: "Kontakt",
            logOut: "Odjava"
        },

        admin: {
            pageTitle: "Administracija",
            panelTitle: "Administracijski panel",

            externalRestTitle: "Vanjski REST servis",
            externalRestDescription: "Dohvaćanje najnovijeg EUR/USD tečaja s vanjskog REST API-ja.",
            loadExchangeRate: "Učitaj EUR/USD tečaj",

            pdfBatchTitle: "Potvrde narudžbi",
            pdfBatchDescription: "Generiranje PDF potvrda za narudžbe kupaca i spremanje u ZIP arhivu.",
            generatePdfBatch: "Generiraj potvrde",
            benchmarkPdfBatch: "Pokreni analizu performansi",

            settingsManagement: "Postavke aplikacije",
            settingsLanguage: "Zadani jezik",
            settingsTheme: "Tema",
            settingsItemsPerPage: "Proizvoda po stranici",
            themeDark: "Tamna",
            themeLight: "Svijetla",
            saveSettings: "Spremi postavke",

            userManagement: "Upravljanje korisnicima",
            searchUsersPlaceholder: "Pretraži korisnike po korisničkom imenu ili emailu",
            allRoles: "Sve uloge",
            adminRole: "Admin",
            customerRole: "Kupac",

            createSecureLogArchive: "Izradi sigurnu arhivu",
            decryptSecureLogArchive: "Dešifriraj sigurnu arhivu",

            systemToolsTitle: "Održavanje i sigurnost sustava",
            systemToolsDescription: "Alati za održavanje inventarnih datoteka, zaštitu logova, provjeru integriteta i analizu sustava.",

            activityLogsTitle: "Aktivnosni logovi",
            activityLogsDescription: "Pregled sigurnosno relevantnih aktivnosti aplikacije spremljenih u JSON formatu.",
            loadActivityLogs: "Učitaj logove",
            hashActivityLogs: "Hashiraj logove",
            encryptActivityLogs: "Šifriraj logove",
            decryptActivityLogs: "Dešifriraj logove",

            stockXmlTitle: "XML zaliha",
            stockXmlDescription: "Izvoz i pregled stanja zaliha proizvoda spremljenog u XML formatu.",
            exportStockXml: "Izvezi XML zaliha",
            loadStockXml: "Učitaj XML zaliha",
            hashStockXml: "Hashiraj XML zaliha",

            analysisTitle: "Analiza sustava",
            analysisDescription: "Pokretanje paralelne analize proizvoda i narudžbi korištenjem bazena dretvi.",
            runAnalysis: "Pokreni analizu",
            loadAnalysisResults: "Učitaj rezultate analize",

            binaryBackupTitle: "Binarna kopija zaliha",
            binaryBackupDescription: "Izrada i čitanje prilagođene binarne datoteke sa stanjem zaliha.",
            writeBinaryBackup: "Izradi binarnu kopiju",
            readBinaryBackup: "Učitaj binarnu kopiju",

            loadRecentActivityLogs: "Pregled trenutnih aktivnosti",
            loadArchivedActivityLogs: "Pregled arhiviranih aktivnosti",
            verifyAuditIntegrity: "Provjeri integritet audit logova",

            toolOutputTitle: "Rezultat",

            orderManagement: "Upravljanje narudžbama",

            categoryManagement: "Upravljanje kategorijama",
            categoryNamePlaceholder: "Naziv kategorije",
            addCategory: "Dodaj kategoriju",
            cancelCategoryEdit: "Odustani od uređivanja",

            productManagement: "Upravljanje proizvodima",
            productNamePlaceholder: "Naziv proizvoda",
            productBrandPlaceholder: "Brend",
            selectCategory: "Odaberi kategoriju",
            productPricePlaceholder: "Cijena",
            productStockPlaceholder: "Zaliha",
            productDescriptionPlaceholder: "Opis",
            addProduct: "Dodaj proizvod",
            cancelProductEdit: "Odustani od uređivanja",

            userTable: {
                id: "ID",
                username: "Korisničko ime",
                email: "Email",
                role: "Uloga",
                changeRole: "Promijeni ulogu",
                actions: "Akcije",
                noUsers: "Nema pronađenih korisnika."
            },

            orderTable: {
                id: "ID",
                user: "Korisnik",
                email: "Email",
                date: "Datum",
                status: "Status",
                total: "Ukupno",
                changeStatus: "Promijeni status",
                noOrders: "Nema pronađenih narudžbi."
            },

            categoryTable: {
                id: "ID",
                name: "Naziv",
                actions: "Akcije",
                noCategories: "Nema pronađenih kategorija."
            },

            productTable: {
                id: "ID",
                name: "Naziv",
                brand: "Brend",
                category: "Kategorija",
                price: "Cijena",
                stock: "Zaliha",
                image: "Slika",
                actions: "Akcije",
                noProducts: "Nema pronađenih proizvoda.",
                noImage: "Nema slike"
            },

            messages: {
                accessDenied: "Pristup odbijen.",
                notAuthenticated: "Niste prijavljeni.",
                notAuthorized: "Nemate ovlasti za pristup ovoj stranici.",

                systemToolError: "Akcija održavanja sustava nije uspjela.",

                settingsSaved: "Postavke su uspješno spremljene.",
                settingsLoadError: "Greška pri učitavanju postavki.",
                settingsSaveError: "Greška pri spremanju postavki.",

                confirmDeleteUser: "Jeste li sigurni da želite obrisati ovog korisnika?",
                userRoleUpdated: "Uloga korisnika je ažurirana.",
                userDeleted: "Korisnik je uspješno obrisan.",
                userRoleUpdateError: "Greška pri ažuriranju uloge korisnika.",
                userDeleteError: "Greška pri brisanju korisnika.",

                orderStatusUpdated: "Status narudžbe je ažuriran.",
                orderStatusUpdateError: "Greška pri ažuriranju statusa narudžbe.",

                loadInventorySnapshot: "Pregled XML snapshota zaliha",
                verifyInventorySnapshotIntegrity: "Provjeri integritet XML snapshota",

                categoryNameRequired: "Naziv kategorije je obavezan.",
                categoryNotFound: "Kategorija nije pronađena.",
                confirmDeleteCategory: "Jeste li sigurni da želite obrisati ovu kategoriju?",
                categoryAdded: "Kategorija je uspješno dodana.",
                categoryUpdated: "Kategorija je uspješno ažurirana.",
                categoryDeleted: "Kategorija je uspješno obrisana.",
                categorySaveError: "Greška pri spremanju kategorije.",
                categoryDeleteError: "Greška pri brisanju kategorije.",

                productNotFound: "Proizvod nije pronađen.",
                productRequiredFields: "Molim ispunite sva obavezna polja proizvoda.",
                confirmDeleteProduct: "Jeste li sigurni da želite obrisati ovaj proizvod?",
                productAdded: "Proizvod je uspješno dodan.",
                productUpdated: "Proizvod je uspješno ažuriran.",
                productDeleted: "Proizvod je uspješno obrisan.",
                productSaveError: "Greška pri spremanju proizvoda.",
                productDeleteError: "Greška pri brisanju proizvoda."
            },

            statuses: {
                NEW: "Nova",
                PROCESSING: "U obradi",
                SHIPPED: "Poslana",
                DELIVERED: "Dostavljena",
                CANCELLED: "Otkazana"
            }
        },

        profile: {
            pageTitle: "Moj profil",
            heading: "Moj profil",
            emailLabel: "Email:",
            usernameLabel: "Korisničko ime:",
            roleLabel: "Uloga:",
            updateProfileButton: "Ažuriraj profil",

            appearanceTitle: "Izgled aplikacije",
            appearanceDescription: "Odaberite kako aplikacija izgleda na ovom uređaju.",
            lightTheme: "Svijetla",
            darkTheme: "Tamna",

            updateModalTitle: "Ažuriraj svoj profil",
            saveChanges: "Spremi promjene",

            changePasswordTitle: "Promjena lozinke",
            newPasswordLabel: "Nova lozinka:",
            confirmPasswordLabel: "Potvrdi lozinku:",
            changePasswordButton: "Promijeni lozinku",

            myOrdersTitle: "Moje narudžbe",
            loadingOrders: "Učitavanje narudžbi...",
            noOrders: "Još nemate narudžbi.",
            deleteAccountButton: "Obriši račun",

            order: {
                orderId: "ID narudžbe",
                date: "Datum",
                status: "Status",
                total: "Ukupno",
                downloadPdf: "Preuzmi PDF",
                items: "Stavke",
                product: "Proizvod",
                quantity: "Količina",
                unitPrice: "Jedinična cijena",
                subtotal: "Međuzbroj"
            },

            messages: {
                notLoggedIn: "Niste prijavljeni.",
                themeSaved: "Postavka teme je spremljena.",
                profileUpdated: "Profil je uspješno ažuriran.",
                profileUpdateError: "Greška pri ažuriranju profila.",
                passwordsDoNotMatch: "Lozinke se ne podudaraju.",
                passwordRequired: "Lozinka je obavezna.",
                passwordUpdated: "Lozinka je uspješno promijenjena.",
                passwordUpdateError: "Greška pri promjeni lozinke.",
                confirmDeleteAccount: "Jeste li sigurni da želite obrisati svoj račun?",
                accountDeleted: "Račun je uspješno obrisan.",
                accountDeleteError: "Greška pri brisanju računa.",
                ordersLoadError: "Greška pri učitavanju narudžbi."
            }
        },

        cart: {
            pageTitle: "Moja košarica",
            heading: "Moja košarica",
            totalPrice: "Ukupna cijena",
            checkout: "Dovrši kupnju",
            continueShopping: "Nastavi kupovati",
            emptyCart: "Vaša košarica je prazna.",
            remove: "Ukloni",
            updateQuantity: "Ažuriraj količinu",
            product: "Proizvod",
            price: "Cijena",
            quantity: "Količina",
            subtotal: "Međuzbroj",

            messages: {
                notLoggedIn: "Morate se prijaviti za pregled košarice.",
                loadError: "Greška pri učitavanju košarice.",
                confirmRemove: "Želite li ukloniti ovu stavku iz košarice?",
                removeError: "Greška pri uklanjanju stavke iz košarice.",
                quantityUpdateError: "Greška pri ažuriranju količine.",
                checkoutSuccess: "Kupnja je uspješno završena.",
                checkoutError: "Greška tijekom završetka kupnje.",
                emptyCartCheckout: "Vaša košarica je prazna."
            }
        },

        contact: {
            pageTitle: "Kontakt",
            locationTitle: "Lokacija",
            contactTitle: "Kontaktirajte nas",
            phoneNumber: "Broj telefona:",
            email: "E-mail:"
        },

        registration: {
            pageTitle: "Registracija",
            heading: "Izrada korisničkog računa",
            emailLabel: "Email:",
            usernameLabel: "Korisničko ime:",
            usernamePlaceholder: "Odaberite korisničko ime",
            passwordLabel: "Lozinka:",
            registerButton: "Registriraj se",

            messages: {
                success: "Registracija je uspješna.",
                error: "Registracija nije uspjela.",
                emailRequired: "Email je obavezan.",
                usernameRequired: "Korisničko ime je obavezno.",
                passwordRequired: "Lozinka je obavezna."
            }
        },

        signIn: {
            pageTitle: "Prijava",
            heading: "Prijava",
            usernameLabel: "Korisničko ime:",
            passwordLabel: "Lozinka:",
            signInButton: "Prijavi se",
            noAccountText: "Nemate korisnički račun?",
            registerText: "Registrirajte se",
            hereText: "ovdje.",

            messages: {
                success: "Uspješno ste prijavljeni.",
                error: "Neispravno korisničko ime ili lozinka.",
                usernameRequired: "Korisničko ime je obavezno.",
                passwordRequired: "Lozinka je obavezna."
            }
        },

        products: {
            heading: "Naši proizvodi",
            brand: "Brend",
            category: "Kategorija",
            addToCart: "Dodaj u košaricu",
            inStock: "Na zalihi",
            outOfStock: "Nema na zalihi",
            noProducts: "Trenutno nema dostupnih proizvoda.",
            loadError: "Došlo je do greške pri učitavanju proizvoda.",
            unknownCategory: "Nepoznata kategorija",
            noDescription: "Opis nije dostupan.",

            searchPlaceholder: "Pretraži proizvode...",
            allCategories: "Sve kategorije",
            allStock: "Sve zalihe",
            inStockOnly: "Dostupno",
            outOfStockOnly: "Nedostupno",
            sortDefault: "Zadano sortiranje",
            sortPriceAsc: "Cijena: od niže prema višoj",
            sortPriceDesc: "Cijena: od više prema nižoj",
            sortNameAsc: "Naziv: A-Z",
            sortNameDesc: "Naziv: Z-A",
            sortStockDesc: "Zaliha: od veće prema manjoj",
            clearFilters: "Očisti filtere",
            noMatchingProducts: "Nijedan proizvod ne odgovara filterima.",
            showing: "Prikaz",
            of: "od",
            noResultsInfo: "Nema pronađenih proizvoda",

            messages: {
                notLoggedIn: "Morate se prijaviti za dodavanje proizvoda u košaricu.",
                addedToCart: "Proizvod dodan u košaricu!",
                alreadyInCart: "Proizvod je već u košarici.",
                genericError: "Došlo je do greške. Pokušajte ponovno."
            }
        }
    }
};