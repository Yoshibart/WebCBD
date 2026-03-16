const API_BASE = "/api/ecommerce/v1";
const CART_ID_STORAGE_KEY = "webcbd_guest_cart_id";
const AUTH_STORAGE_KEY = "webcbd_auth_state";

const state = {
    products: [],
    productsLoading: true,
    productsLoadFailed: false,
    cartId: window.localStorage.getItem(CART_ID_STORAGE_KEY) || "",
    cartProductIds: [],
    auth: {
        token: "",
        tokenType: "Bearer",
        username: "",
        email: "",
        role: "",
        expiresAt: 0
    }
};

const carouselTimers = new Map();
let adminProductsTable = null;
let categoryChart = null;
let resizeDebounceTimer = null;

$(function () {
    bindAppEvents();
    initializeAuth();
    refreshUi();
    loadProducts();

    if (state.cartId) {
        fetchCart(state.cartId);
    }
});

function bindAppEvents() {
    $(window).on("resize", handleCarouselResize);
    $(document).on("click", ".js-add-to-cart", handleAddToCart);
    $(document).on("click", ".js-remove-from-cart", handleRemoveFromCart);
    $(document).on("click", ".js-auth-action", handleAuthAction);
    $(document).on("submit", "#login-form", handleLoginSubmit);
    $(document).on("click", ".js-admin-toggle", handleAdminToggle);
    $(document).on("click", ".js-carousel-prev", handleCarouselPrev);
    $(document).on("click", ".js-carousel-next", handleCarouselNext);
    $(document).on("click", ".js-category-link", handleCategoryLink);
    $(document).on("shown.bs.modal", "#adminProductsModal", handleAdminModalShown);
}

function refreshUi() {
    renderShopView();
    renderCartSidebar();
    syncProductButtons();
    syncAuthUi();
    renderAdminProductsTable();
}

function loadProducts() {
    state.productsLoading = true;
    state.productsLoadFailed = false;
    renderShopView();

    return $.ajax({
        url: `${API_BASE}/products`,
        method: "GET",
        dataType: "json"
    }).done(function (products) {
        state.products = Array.isArray(products) ? products.slice() : [];
        state.productsLoading = false;
        state.productsLoadFailed = false;
        refreshUi();
    }).fail(function (xhr) {
        state.products = [];
        state.productsLoading = false;
        state.productsLoadFailed = true;
        refreshUi();
        showToast(getErrorMessage(xhr, "Product loading failed."));
    });
}

function fetchCart(cartId) {
    $.ajax({
        url: `${API_BASE}/carts/${encodeURIComponent(cartId)}`,
        method: "GET",
        dataType: "json"
    }).done(function (cart) {
        hydrateCart(cart);
    }).fail(function () {
        state.cartId = "";
        state.cartProductIds = [];
        window.localStorage.removeItem(CART_ID_STORAGE_KEY);
        refreshUi();
    });
}

function ensureCart() {
    if (state.cartId) {
        return $.Deferred().resolve(state.cartId).promise();
    }

    return $.ajax({
        url: `${API_BASE}/carts`,
        method: "POST",
        dataType: "json"
    }).then(function (cart) {
        hydrateCart(cart);
        showToast("Guest cart created.");
        return state.cartId;
    });
}

function hydrateCart(cart) {
    state.cartId = cart.cartId || "";
    state.cartProductIds = Array.isArray(cart.productIds) ? cart.productIds.slice() : [];

    if (state.cartId) {
        window.localStorage.setItem(CART_ID_STORAGE_KEY, state.cartId);
    }

    refreshUi();
}

function initializeAuth() {
    const stored = readStoredAuth();

    if (stored && stored.token) {
        if (isAuthExpired(stored)) {
            clearAuthStorage();
        } else {
            state.auth = {
                token: stored.token || "",
                tokenType: stored.tokenType || "Bearer",
                username: stored.username || "",
                email: stored.email || "",
                role: stored.role || "",
                expiresAt: Number(stored.expiresAt || 0)
            };
        }
    }
}

function readStoredAuth() {
    const sessionValue = window.sessionStorage.getItem(AUTH_STORAGE_KEY);
    const raw = sessionValue;

    if (!raw) {
        return null;
    }

    try {
        return JSON.parse(raw);
    } catch (error) {
        clearAuthStorage();
        return null;
    }
}

function saveAuth() {
    const payload = JSON.stringify(state.auth);
    window.sessionStorage.setItem(AUTH_STORAGE_KEY, payload);
}

function clearAuthStorage() {
    window.sessionStorage.removeItem(AUTH_STORAGE_KEY);
}

function clearAuthState() {
    state.auth = {
        token: "",
        tokenType: "Bearer",
        username: "",
        email: "",
        role: "",
        expiresAt: 0
    };
    clearAuthStorage();
}

function isAuthExpired(auth) {
    return auth && auth.expiresAt && Date.now() > Number(auth.expiresAt);
}

function isLoggedIn() {
    return Boolean(state.auth.token);
}

function isAdmin() {
    return isLoggedIn() && state.auth.role === "ADMIN";
}

function syncAuthUi() {
    const loggedIn = isLoggedIn();
    const $authButton = $(".js-auth-action");
    const $authLabel = $authButton.find(".login-text");
    const $authIcon = $authButton.find(".login-icon .glyphicon");
    const labelText = loggedIn ? "Logout" : "Login";

    $authLabel.text(labelText);
    $authButton.attr("aria-label", labelText);
    if ($authIcon.length) {
        $authIcon
            .toggleClass("glyphicon-log-in", !loggedIn)
            .toggleClass("glyphicon-log-out", loggedIn);
    }

    if (loggedIn) {
        $authButton.removeAttr("data-bs-toggle").removeAttr("data-bs-target");
    } else {
        $authButton.attr("data-bs-toggle", "modal").attr("data-bs-target", "#loginModal");
    }

    const $adminButton = $(".js-admin-toggle");
    const adminVisible = isAdmin();
    $adminButton.toggleClass("d-none", !adminVisible);
}

function handleAuthAction(event) {
    if (!isLoggedIn()) {
        return;
    }

    event.preventDefault();
    performLogout();
}

function handleLoginSubmit(event) {
    event.preventDefault();

    if (isLoggedIn()) {
        return;
    }

    const username = String($("#login-username").val() || "").trim();
    const password = String($("#login-password").val() || "");
    if (!username || !password) {
        showToast("Enter a username and password.");
        return;
    }

    setLoginFormLoading(true);

    performLogin(username, password).always(function () {
        setLoginFormLoading(false);
    });
}

function performLogin(username, password) {
    return $.ajax({
        url: `${API_BASE}/auth/login`,
        method: "POST",
        contentType: "application/json",
        dataType: "json",
        data: JSON.stringify({
            username: username,
            password: password
        })
    }).done(function (authResponse) {
        const expiresIn = Number(authResponse.expiresIn || 0);
        state.auth = {
            token: authResponse.accessToken || "",
            tokenType: authResponse.tokenType || "Bearer",
            username: authResponse.username || username,
            email: authResponse.email || "",
            role: authResponse.role || "",
            expiresAt: expiresIn ? Date.now() + expiresIn * 1000 : 0
        };
        saveAuth();
        syncAuthUi();
        renderAdminProductsTable();
        showToast(`Welcome ${state.auth.username || "back"}.`);
        $("#login-password").val("");
        const modalElement = document.getElementById("loginModal");
        if (modalElement) {
            const modal = bootstrap.Modal.getOrCreateInstance(modalElement);
            modal.hide();
        }
    }).fail(function (xhr) {
        showToast(getErrorMessage(xhr, "Login failed."));
    });
}

function performLogout() {
    const token = state.auth.token;
    const tokenType = state.auth.tokenType || "Bearer";
    const request = token ? $.ajax({
        url: `${API_BASE}/auth/logout`,
        method: "POST",
        headers: {
            Authorization: `${tokenType} ${token}`
        }
    }) : $.Deferred().resolve().promise();

    return request.always(function () {
        clearAuthState();
        syncAuthUi();
        renderAdminProductsTable();
        showToast("Logged out.");
    });
}

function setLoginFormLoading(isLoading) {
    const $form = $("#login-form");
    $form.find("input, button").prop("disabled", isLoading);
    const $submit = $form.find("button[type=\"submit\"]");
    $submit.text(isLoading ? "Signing in..." : "Sign in");
}

function handleAdminToggle() {
    if (!isAdmin()) {
        return;
    }
    renderAdminProductsTable();
    const modalElement = document.getElementById("adminProductsModal");
    if (modalElement) {
        const modal = bootstrap.Modal.getOrCreateInstance(modalElement);
        modal.show();
    }
}

function renderAdminProductsTable() {
    if (!isAdmin()) {
        const modalElement = document.getElementById("adminProductsModal");
        if (modalElement) {
            const modal = bootstrap.Modal.getInstance(modalElement);
            if (modal) {
                modal.hide();
            }
        }
        destroyAdminTable();
        destroyAdminChart();
        return;
    }

    const $state = $("#admin-products-state");
    const $table = $("#admin-products-table");
    const $tbody = $("#admin-products-table-body");

    if (state.productsLoading) {
        $state.text("Loading products...").removeClass("d-none");
        $table.addClass("d-none");
        destroyAdminTable();
        renderAdminChart();
        return;
    }

    if (state.productsLoadFailed) {
        $state.text("Product data could not be loaded.").removeClass("d-none");
        $table.addClass("d-none");
        destroyAdminTable();
        renderAdminChart();
        return;
    }

    if (!state.products.length) {
        $state.text("No products available.").removeClass("d-none");
        $table.addClass("d-none");
        destroyAdminTable();
        renderAdminChart();
        return;
    }

    $state.addClass("d-none");
    $table.removeClass("d-none");
    $tbody.empty();

    state.products.forEach(function (product) {
        $tbody.append(`
            <tr>
                <td>${Number(product.id)}</td>
                <td>${escapeHtml(product.name)}</td>
                <td>${escapeHtml(product.category)}</td>
                <td>${formatPrice(product.price)}</td>
                <td>${escapeHtml(product.description)}</td>
            </tr>
        `);
    });

    initializeAdminTable();
    renderAdminChart();
}

function handleAdminModalShown() {
    if ($.fn.DataTable && $.fn.dataTable.isDataTable("#admin-products-table")) {
        $("#admin-products-table").DataTable().columns.adjust();
    }
    if (categoryChart) {
        categoryChart.resize();
    }
}

function initializeAdminTable() {
    if (!$.fn.DataTable) {
        return;
    }

    if ($.fn.dataTable.isDataTable("#admin-products-table")) {
        $("#admin-products-table").DataTable().destroy();
    }

    adminProductsTable = $("#admin-products-table").DataTable({
        pageLength: 6,
        lengthChange: false,
        info: false,
        order: [[0, "asc"]]
    });
}

function destroyAdminTable() {
    if (!$.fn.DataTable) {
        return;
    }
    if ($.fn.dataTable.isDataTable("#admin-products-table")) {
        $("#admin-products-table").DataTable().destroy();
    }
    adminProductsTable = null;
}

function renderAdminChart() {
    const canvas = document.getElementById("categoryChart");
    const $state = $("#admin-chart-state");
    const $count = $("#admin-chart-count");

    if (!canvas) {
        return;
    }

    if (!isAdmin()) {
        destroyAdminChart();
        return;
    }

    if (state.productsLoading) {
        $count.text("0 categories");
        showAdminChartState($state, "Loading chart data...");
        destroyAdminChart();
        return;
    }

    if (state.productsLoadFailed) {
        $count.text("0 categories");
        showAdminChartState($state, "Chart data could not be loaded.");
        destroyAdminChart();
        return;
    }

    if (!state.products.length) {
        $count.text("0 categories");
        showAdminChartState($state, "No products available for chart.");
        destroyAdminChart();
        return;
    }

    const stats = buildCategoryStats(state.products);
    $count.text(`${stats.labels.length} categor${stats.labels.length === 1 ? "y" : "ies"}`);
    $state.addClass("d-none").text("");

    if (!window.Chart) {
        showAdminChartState($state, "Chart library not available.");
        destroyAdminChart();
        return;
    }

    destroyAdminChart();

    categoryChart = new Chart(canvas, {
        type: "pie",
        data: {
            labels: stats.labels,
            datasets: [{
                label: "Products",
                data: stats.counts,
                backgroundColor: stats.colors
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: "bottom"
                }
            }
        }
    });
}

function destroyAdminChart() {
    if (categoryChart) {
        categoryChart.destroy();
        categoryChart = null;
    }
}

function showAdminChartState($state, message) {
    $state.text(message).removeClass("d-none");
}

function buildCategoryStats(products) {
    const grouped = getGroupedProducts(products);
    const labels = Object.keys(grouped);
    const counts = labels.map(function (label) {
        return grouped[label].length;
    });
    const colors = labels.map(function (_, index) {
        const palette = ["#0f766e", "#f08a24", "#1f6f78", "#d97706", "#0ea5e9", "#7c3aed"];
        return palette[index % palette.length];
    });

    return { labels: labels, counts: counts, colors: colors };
}

function renderShopView() {
    const groupedProducts = getGroupedProducts(state.products);
    const categories = Object.keys(groupedProducts);

    if (state.productsLoading) {
        clearCarouselTimers();
        $("#category-pills").empty();
        $("#catalog-sections").empty();
        setCatalogState("loading", "Loading products...");
        return;
    }

    if (state.productsLoadFailed) {
        clearCarouselTimers();
        $("#category-pills").empty();
        $("#catalog-sections").empty();
        setCatalogState("error", "The product catalog could not be loaded.");
        return;
    }

    if (!state.products.length) {
        clearCarouselTimers();
        $("#category-pills").empty();
        $("#catalog-sections").empty();
        setCatalogState("empty", "No products are available right now.");
        return;
    }

    $("#catalog-state").hide();
    renderCategoryLinks(groupedProducts);
    renderCatalogSections(groupedProducts);
}

function renderCategoryLinks(groupedProducts) {
    const categories = Object.keys(groupedProducts);
    const $container = $("#category-pills");

    $container.empty();

    if (!categories.length) {
        $container.html('<p class="section-copy mb-0">No categories available yet.</p>');
        return;
    }

    categories.forEach(function (category, index) {
        $container.append(`
            <a class="category-chip js-category-link" href="#category-${slugify(category)}" data-category="${escapeHtml(category)}">
                <span class="chip-dot theme-${index % 6}"></span>
                <span>${escapeHtml(category)}</span>
                <span class="chip-count">${groupedProducts[category].length}</span>
            </a>
        `);
    });
}

function handleCategoryLink(event) {
    event.preventDefault();
    const category = String($(this).data("category") || "");
    scrollToCategory(category);
}

function scrollToCategory(category) {
    if (!category) {
        return;
    }

    const categoryElement = document.getElementById(`category-${slugify(category)}`);
    if (categoryElement) {
        categoryElement.scrollIntoView({ behavior: "smooth", block: "start" });
    }
}

function renderCatalogSections(groupedProducts) {
    const $container = $("#catalog-sections");
    clearCarouselTimers();
    $container.empty();

    Object.entries(groupedProducts).forEach(function ([category, products], categoryIndex) {
        const themeIndex = categoryIndex % 6;
        const controls = products.length > 1 ? `
            <button class="carousel-arrow carousel-arrow-prev js-carousel-prev" type="button" aria-label="Previous products">
                <span aria-hidden="true">&#8249;</span>
            </button>
            <button class="carousel-arrow carousel-arrow-next js-carousel-next" type="button" aria-label="Next products">
                <span aria-hidden="true">&#8250;</span>
            </button>
        ` : "";
        const productsHtml = products.map(function (product) {
            return `
                <div class="product-cell">
                    ${createProductCard(product, themeIndex)}
                </div>
            `;
        }).join("");

        $container.append(`
            <section class="category-section" id="category-${slugify(category)}">
                <div class="category-header">
                    <div>
                        <h3 class="category-title mb-1">${escapeHtml(category)}</h3>
                        <p class="category-subtitle mb-0">${products.length} product${products.length === 1 ? "" : "s"} available</p>
                    </div>
                    <span class="category-badge">${products.length} item${products.length === 1 ? "" : "s"}</span>
                </div>
                <div id="carousel-${slugify(category)}" class="product-carousel js-product-carousel" data-current-index="0">
                    ${controls}
                    <div class="product-viewport">
                        <div class="product-track">
                            ${productsHtml}
                        </div>
                    </div>
                </div>
            </section>
        `);
    });

    initializeCarousels();
    syncProductButtons();
}

function initializeCarousels() {
    $("#catalog-sections .js-product-carousel").each(function () {
        const $carousel = $(this);
        updateCarouselLayout($carousel);
        bindCarouselHover($carousel);
        startCarouselTimer($carousel);
    });
}

function bindCarouselHover($carousel) {
    $carousel.off(".carouselAuto");
    $carousel.on("mouseenter.carouselAuto", function () {
        stopCarouselTimer($carousel);
    });
    $carousel.on("mouseleave.carouselAuto", function () {
        startCarouselTimer($carousel);
    });
}

function handleCarouselPrev() {
    moveCarousel($(this).closest(".js-product-carousel"), -1);
}

function handleCarouselNext() {
    moveCarousel($(this).closest(".js-product-carousel"), 1);
}

function handleCarouselResize() {
    window.clearTimeout(resizeDebounceTimer);
    resizeDebounceTimer = window.setTimeout(function () {
        $("#catalog-sections .js-product-carousel").each(function () {
            const $carousel = $(this);
            updateCarouselLayout($carousel);
            restartCarouselTimer($carousel);
        });
    }, 120);
}

function moveCarousel($carousel, step) {
    const maxIndex = getMaxCarouselIndex($carousel);

    if (maxIndex <= 0) {
        updateCarouselLayout($carousel);
        return;
    }

    const currentIndex = Number($carousel.attr("data-current-index")) || 0;
    let nextIndex = currentIndex + step;

    if (nextIndex < 0) {
        nextIndex = maxIndex;
    } else if (nextIndex > maxIndex) {
        nextIndex = 0;
    }

    updateCarouselLayout($carousel, nextIndex);
    restartCarouselTimer($carousel);
}

function updateCarouselLayout($carousel, requestedIndex) {
    const $viewport = $carousel.find(".product-viewport");
    const $track = $carousel.find(".product-track");
    const $cells = $carousel.find(".product-cell");
    const visibleCount = getVisibleProductCount();
    const cellCount = $cells.length;
    const maxIndex = Math.max(0, cellCount - visibleCount);
    const currentIndex = Math.min(
        maxIndex,
        typeof requestedIndex === "number" ? requestedIndex : Number($carousel.attr("data-current-index")) || 0
    );
    const viewportWidth = $viewport.innerWidth();
    const cellWidth = visibleCount > 0 ? viewportWidth / visibleCount : viewportWidth;

    $carousel.attr("data-current-index", currentIndex);
    $cells.css("width", `${cellWidth}px`);
    $track.css("transform", `translateX(-${currentIndex * cellWidth}px)`);
    $carousel.find(".carousel-arrow")
        .prop("disabled", maxIndex === 0)
        .toggleClass("is-disabled", maxIndex === 0);

    if (maxIndex === 0) {
        stopCarouselTimer($carousel);
    }
}

function getVisibleProductCount() {
    if (window.innerWidth < 768) {
        return 1;
    }

    if (window.innerWidth < 1200) {
        return 2;
    }

    return 3;
}

function getMaxCarouselIndex($carousel) {
    return Math.max(0, $carousel.find(".product-cell").length - getVisibleProductCount());
}

function startCarouselTimer($carousel) {
    const maxIndex = getMaxCarouselIndex($carousel);

    stopCarouselTimer($carousel);

    if (maxIndex <= 0) {
        return;
    }

    const timerId = window.setInterval(function () {
        if (!document.body.contains($carousel.get(0))) {
            stopCarouselTimer($carousel);
            return;
        }

        moveCarousel($carousel, 1);
    }, 3500);

    carouselTimers.set($carousel.attr("id"), timerId);
}

function stopCarouselTimer($carousel) {
    const carouselId = $carousel.attr("id");
    const timerId = carouselTimers.get(carouselId);

    if (timerId) {
        window.clearInterval(timerId);
        carouselTimers.delete(carouselId);
    }
}

function restartCarouselTimer($carousel) {
    stopCarouselTimer($carousel);
    startCarouselTimer($carousel);
}

function clearCarouselTimers() {
    carouselTimers.forEach(function (timerId) {
        window.clearInterval(timerId);
    });
    carouselTimers.clear();
}

function handleAddToCart() {
    const $button = $(this);
    const productId = Number($button.data("productId"));

    if (!productId || state.cartProductIds.includes(productId)) {
        return;
    }

    $button.prop("disabled", true).text("Adding...");

    ensureCart()
        .then(function (cartId) {
            return $.ajax({
                url: `${API_BASE}/carts/${encodeURIComponent(cartId)}/products/${productId}`,
                method: "POST",
                dataType: "json"
            });
        })
        .done(function (cart) {
            hydrateCart(cart);
            showToast("Product added to cart.");
        })
        .fail(function (xhr) {
            showToast(getErrorMessage(xhr, "Adding this product failed."));
        })
        .always(function () {
            $button.prop("disabled", false);
            syncProductButtons();
        });
}

function handleRemoveFromCart() {
    const $button = $(this);
    const productId = Number($button.data("productId"));

    if (!state.cartId || !productId || !state.cartProductIds.includes(productId)) {
        return;
    }

    $button.prop("disabled", true).text("Removing...");

    $.ajax({
        url: `${API_BASE}/carts/${encodeURIComponent(state.cartId)}/products/${productId}`,
        method: "DELETE",
        dataType: "json"
    }).done(function (cart) {
        hydrateCart(cart);
        showToast("Product removed from cart.");
    }).fail(function (xhr) {
        showToast(getErrorMessage(xhr, "Removing this product failed."));
    }).always(function () {
        $button.prop("disabled", false).text("Remove");
        syncProductButtons();
    });
}

function getSelectedProducts() {
    const productMap = new Map(state.products.map(function (product) {
        return [Number(product.id), product];
    }));

    return state.cartProductIds.map(function (productId) {
        return productMap.get(Number(productId));
    }).filter(Boolean);
}

function renderCartSidebar() {
    const selectedProducts = getSelectedProducts();
    const cartTotal = getCartTotal(selectedProducts);
    const $container = $("#cart-items");

    $("#cart-sidebar-count").text(`${selectedProducts.length} item${selectedProducts.length === 1 ? "" : "s"}`);
    $("#cart-total-amount").text(formatPrice(cartTotal));

    $container.empty();

    if (!selectedProducts.length) {
        $container.append('<p class="empty-cart-message mb-0">Cart Empty</p>');
        return;
    }

    selectedProducts.forEach(function (product) {
        $container.append(`
            <article class="cart-item d-flex align-items-start justify-content-between gap-2">
                <div>
                    <h4 class="cart-product-name h6">${escapeHtml(product.name)}</h4>
                    <p class="cart-product-meta">${escapeHtml(product.category)} | ${formatPrice(product.price)}</p>
                </div>
                <button class="btn btn-sm btn-outline-danger js-remove-from-cart" type="button" data-product-id="${Number(product.id)}">
                    Remove
                </button>
            </article>
        `);
    });
}

function getCartTotal(products) {
    return products.reduce(function (sum, product) {
        return sum + Number(product.price || 0);
    }, 0);
}

function createProductCard(product, themeIndex) {
    const productId = Number(product.id);
    const isInCart = state.cartProductIds.includes(productId);
    const buttonClass = isInCart ? "btn-outline-secondary" : "btn-dark";
    const buttonText = isInCart ? "In cart" : "Add to cart";

    return `
        <article class="product-card">
            <div class="product-media theme-${themeIndex}">
                <div class="media-copy">
                    <span class="media-eyebrow">${escapeHtml(product.category)}</span>
                </div>
                <span class="media-monogram">${escapeHtml(getMonogram(product.name))}</span>
            </div>
            <div class="product-body">
                <h4 class="product-name">${escapeHtml(product.name)}</h4>
                <p class="product-description">${escapeHtml(product.description)}</p>
                <div class="product-meta">
                    <span class="price-tag">${formatPrice(product.price)}</span>
                    <button
                        class="btn ${buttonClass} btn-add-cart js-add-to-cart"
                        type="button"
                        data-product-id="${productId}"
                        ${isInCart ? "disabled" : ""}>
                        ${buttonText}
                    </button>
                </div>
            </div>
        </article>
    `;
}

function syncProductButtons() {
    $(".js-add-to-cart").each(function () {
        const $button = $(this);
        const productId = Number($button.data("productId"));
        const isInCart = state.cartProductIds.includes(productId);

        $button
            .toggleClass("btn-dark", !isInCart)
            .toggleClass("btn-outline-secondary", isInCart)
            .prop("disabled", isInCart)
            .text(isInCart ? "In cart" : "Add to cart");
    });
}

function getGroupedProducts(products) {
    return products
        .slice()
        .sort(function (a, b) {
            const categoryCompare = String(a.category).localeCompare(String(b.category));
            return categoryCompare !== 0 ? categoryCompare : String(a.name).localeCompare(String(b.name));
        })
        .reduce(function (grouped, product) {
            const category = product.category || "Uncategorized";
            if (!grouped[category]) {
                grouped[category] = [];
            }
            grouped[category].push(product);
            return grouped;
        }, {});
}

function setCatalogState(type, message) {
    const content = {
        loading: '<div class="spinner-border text-warning" role="status" aria-hidden="true"></div>',
        empty: '<div class="display-6">0</div>',
        error: '<div class="display-6">!</div>'
    };

    $("#catalog-state .card-body").html(`
        ${content[type] || ""}
        <p class="mt-3 mb-0">${escapeHtml(message)}</p>
    `);
    $("#catalog-state").show();
}

function slugify(value) {
    return String(value)
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, "-")
        .replace(/^-|-$/g, "");
}

function getMonogram(name) {
    return String(name || "")
        .split(/\s+/)
        .filter(Boolean)
        .slice(0, 2)
        .map(function (part) {
            return part[0].toUpperCase();
        })
        .join("") || "P";
}

function formatPrice(price) {
    const numericPrice = Number(price || 0);
    return new Intl.NumberFormat("en-IE", {
        style: "currency",
        currency: "EUR"
    }).format(numericPrice);
}

function getErrorMessage(xhr, fallback) {
    if (xhr && xhr.responseJSON && xhr.responseJSON.message) {
        return xhr.responseJSON.message;
    }

    return fallback;
}

function escapeHtml(value) {
    return String(value == null ? "" : value)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

function showToast(message) {
    $("#toast-message").text(message);
    const toastElement = document.getElementById("app-toast");
    const toast = bootstrap.Toast.getOrCreateInstance(toastElement, { delay: 2500 });
    toast.show();
}
