package edu.nuist.ojs.middle.workflow.payment;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.styledxmlparser.css.media.MediaDeviceDescription;
import com.itextpdf.styledxmlparser.css.media.MediaType;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.Coupon;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;

@Component
public class StripeService {

    @Value("${payment.stripe.apiKey}")
    private String apiKey;

	public String getRecieptFile(String chgId) throws StripeException {
			Stripe.apiKey = apiKey;
			Charge charge = Charge.retrieve(chgId);
			String url = charge.getReceiptUrl();
			return url;
	}

	public String createCustomer(String email, String token) {

		String id = null;

		try {
			Stripe.apiKey = apiKey;
			Map<String, Object> customerParams = new HashMap<>();
			customerParams.put("description", "Customer for " + email);
			customerParams.put("email", email);
			// obtained with stripe.js
			customerParams.put("source", token);

			Customer customer = Customer.create(customerParams);
			id = customer.getId();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return id;
	}

	public String createSubscription(String customerId, String plan, String coupon) {

		String subscriptionId = null;

		try {
			Stripe.apiKey = apiKey;

			Map<String, Object> item = new HashMap<>();
			item.put("plan", plan);

			Map<String, Object> items = new HashMap<>();
			items.put("0", item);

			Map<String, Object> params = new HashMap<>();
			params.put("customer", customerId);
			params.put("items", items);

			if (!coupon.isEmpty()) {
				params.put("coupon", coupon);
			}

			Subscription subscription = Subscription.create(params);

			subscriptionId = subscription.getId();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return subscriptionId;
	}
	
	public boolean cancelSubscription(String subscriptionId) {
		
		boolean subscriptionStatus;
		
		try {
			Subscription subscription = Subscription.retrieve(subscriptionId);
			subscription.cancel();
			subscriptionStatus = true;	
		} catch (Exception e) {
			e.printStackTrace();
			subscriptionStatus = false;
		}
		return subscriptionStatus;
	}
	
	public Coupon retriveCoupon(String code) {
		try {
			Stripe.apiKey = apiKey;
			return Coupon.retrieve(code);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public Charge createCharge(long aid, String token, int amount) {
		
		String chargeId = null;
		
		try {
			Stripe.apiKey = apiKey;
			
			Map<String, Object> chargeParams = new HashMap<>();
			chargeParams.put("description","Charge for Article Id:"+ aid);
			chargeParams.put("currency","usd");
			chargeParams.put("amount",amount);
			chargeParams.put("source",token);
			
			Charge charge = Charge.create(chargeParams);
			
		    return charge;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
