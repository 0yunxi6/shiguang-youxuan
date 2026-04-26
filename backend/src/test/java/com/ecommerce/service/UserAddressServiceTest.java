package com.ecommerce.service;

import com.ecommerce.entity.User;
import com.ecommerce.entity.UserAddress;
import com.ecommerce.mapper.UserAddressMapper;
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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAddressServiceTest {

    @Mock
    private UserAddressMapper userAddressMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserAddressService userAddressService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", null)
        );
        User user = new User();
        user.setId(8L);
        user.setUsername("testuser");
        when(userMapper.findByUsername("testuser")).thenReturn(user);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createAddress_firstAddress_setsDefaultAndTrimsFields() {
        when(userAddressMapper.selectCount(any())).thenReturn(0L);
        UserAddress request = new UserAddress();
        request.setReceiverName(" 张三 ");
        request.setReceiverPhone("13800138000");
        request.setProvince(" 广东省 ");
        request.setCity("深圳市");
        request.setDistrict("南山区");
        request.setDetail(" 科技园1号 ");

        Result<?> result = userAddressService.createAddress(request);

        ArgumentCaptor<UserAddress> captor = ArgumentCaptor.forClass(UserAddress.class);
        verify(userAddressMapper).insert(captor.capture());
        UserAddress saved = captor.getValue();
        assertEquals(200, result.getCode());
        assertEquals(8L, saved.getUserId());
        assertEquals("张三", saved.getReceiverName());
        assertEquals("广东省", saved.getProvince());
        assertTrue(saved.getIsDefault());
    }

    @Test
    void createAddress_invalidPhone_returnsErrorWithoutInsert() {
        UserAddress request = new UserAddress();
        request.setReceiverName("张三");
        request.setReceiverPhone("123");
        request.setProvince("广东省");
        request.setCity("深圳市");
        request.setDetail("科技园1号");

        Result<?> result = userAddressService.createAddress(request);

        assertEquals(400, result.getCode());
        verify(userAddressMapper, never()).insert(any(UserAddress.class));
    }
}
