package com.ecommerce.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.User;
import com.ecommerce.mapper.CartMapper;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.util.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartMapper cartMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private CartService cartService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", null)
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void clearCart_deletesOnlyCurrentUsersCartItems() {
        User user = new User();
        user.setId(7L);
        user.setUsername("testuser");
        when(userMapper.findByUsername("testuser")).thenReturn(user);

        Result<?> result = cartService.clearCart();

        ArgumentCaptor<QueryWrapper<Cart>> wrapperCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(cartMapper).delete(wrapperCaptor.capture());
        QueryWrapper<Cart> wrapper = wrapperCaptor.getValue();
        assertEquals(200, result.getCode());
        assertTrue(wrapper.getSqlSegment().contains("user_id"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(7L));
    }
}
