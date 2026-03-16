const API_BASE = "/api/ecommerce/v1";
const CART_ID_STORAGE_KEY = "webcbd_guest_cart_id";

const state = {
    products: [],
    productsLoading: true,
    productsLoadFailed: false,
    cartId: window.localStorage.getItem(CART_ID_STORAGE_KEY) || "",
    cartProductIds: []
};

const carouselTimers = new Map();
let resizeDebounceTimer = null;

$(function () {
    bindAppEvents();
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
    $(document).on("click", ".js-carousel-prev", handleCarouselPrev);
    $(document).on("click", ".js-carousel-next", handleCarouselNext);
    $(document).on("click", ".js-category-link", handleCategoryLink);
}

function refreshUi() {
    renderShopView();
    renderCartSidebar();
    syncProductButtons();
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
