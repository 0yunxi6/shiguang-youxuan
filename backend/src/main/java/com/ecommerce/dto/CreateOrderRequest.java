package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public class CreateOrderRequest {
    @NotBlank(message = "收货人不能为空")
    @Size(max = 50, message = "收货人姓名不能超过50个字符")
    private String receiverName;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    private String receiverPhone;

    @NotBlank(message = "收货地址不能为空")
    @Size(max = 500, message = "收货地址不能超过500个字符")
    private String receiverAddress;

    @Size(max = 500, message = "订单备注不能超过500个字符")
    private String remark;

    private Long couponId;

    @Size(max = 50, message = "支付方式不能超过50个字符")
    private String paymentMethod;

    @Size(max = 100, message = "发票抬头不能超过100个字符")
    private String invoiceTitle;

    @Size(max = 50, message = "纳税人识别号不能超过50个字符")
    private String invoiceTaxNo;

    private List<Long> cartItemIds;

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
    public String getReceiverAddress() { return receiverAddress; }
    public void setReceiverAddress(String receiverAddress) { this.receiverAddress = receiverAddress; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Long getCouponId() { return couponId; }
    public void setCouponId(Long couponId) { this.couponId = couponId; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getInvoiceTitle() { return invoiceTitle; }
    public void setInvoiceTitle(String invoiceTitle) { this.invoiceTitle = invoiceTitle; }
    public String getInvoiceTaxNo() { return invoiceTaxNo; }
    public void setInvoiceTaxNo(String invoiceTaxNo) { this.invoiceTaxNo = invoiceTaxNo; }
    public List<Long> getCartItemIds() { return cartItemIds; }
    public void setCartItemIds(List<Long> cartItemIds) { this.cartItemIds = cartItemIds; }
}
