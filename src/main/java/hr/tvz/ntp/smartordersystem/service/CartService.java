package hr.tvz.ntp.smartordersystem.service;

import hr.tvz.ntp.smartordersystem.model.AddToCartRequest;
import hr.tvz.ntp.smartordersystem.model.Cart;
import hr.tvz.ntp.smartordersystem.model.Order;

public interface CartService {
    Cart getCartForUser(Long userId);
    Cart addToCart(Long userId, AddToCartRequest request);
    Cart updateCartItemQuantity(Long userId, Long cartItemId, int quantity);
    void removeCartItem(Long userId, Long cartItemId);
    Order checkout(Long userId);
}
