package tech.bam.RNBraintreeDropIn;

import android.app.Activity;
import androidx.annotation.NonNull;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.braintreepayments.api.DropInClient;
import com.braintreepayments.api.DropInResult;
import com.braintreepayments.api.DropInListener;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.ThreeDSecureInfo;
import com.braintreepayments.api.models.ThreeDSecureAdditionalInformation;
import com.braintreepayments.api.models.ThreeDSecurePostalAddress;
import com.braintreepayments.api.models.ThreeDSecureRequest;
import com.braintreepayments.api.dropin.DropInRequest;

public class RNBraintreeDropInModule extends ReactContextBaseJavaModule implements DropInListener {

    private Promise mPromise;
    private DropInClient dropInClient;

    public RNBraintreeDropInModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @ReactMethod
    public void show(final ReadableMap options, final Promise promise) {
        if (!options.hasKey("clientToken")) {
            promise.reject("NO_CLIENT_TOKEN", "You must provide a client token");
            return;
        }

        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            promise.reject("NO_ACTIVITY", "There is no current activity");
            return;
        }

        dropInClient = new DropInClient(currentActivity, options.getString("clientToken"));
        DropInRequest dropInRequest = new DropInRequest();

        if (options.hasKey("threeDSecure")) {
            final ReadableMap threeDSecureOptions = options.getMap("threeDSecure");
            if (!threeDSecureOptions.hasKey("amount")) {
                promise.reject("NO_3DS_AMOUNT", "You must provide an amount for 3D Secure");
                return;
            }

            String amount = String.valueOf(threeDSecureOptions.getDouble("amount"));

            ThreeDSecureRequest threeDSecureRequest = new ThreeDSecureRequest()
                .amount(amount)
                .versionRequested(ThreeDSecureRequest.VERSION_2);

            if (threeDSecureOptions.hasKey("email")) {
                threeDSecureRequest.setEmail(threeDSecureOptions.getString("email"));
            }

            if (threeDSecureOptions.hasKey("billingAddress")) {
                final ReadableMap threeDSecureBillingAddress = threeDSecureOptions.getMap("billingAddress");
                ThreeDSecurePostalAddress billingAddress = new ThreeDSecurePostalAddress();

                if (threeDSecureBillingAddress.hasKey("givenName")) {
                    billingAddress.setGivenName(threeDSecureBillingAddress.getString("givenName"));
                }

                if (threeDSecureBillingAddress.hasKey("surname")) {
                    billingAddress.setSurname(threeDSecureBillingAddress.getString("surname"));
                }

                if (threeDSecureBillingAddress.hasKey("streetAddress")) {
                    billingAddress.setStreetAddress(threeDSecureBillingAddress.getString("streetAddress"));
                }

                // Other billing address fields...

                threeDSecureRequest.setBillingAddress(billingAddress);
            }

            dropInRequest.setThreeDSecureRequest(threeDSecureRequest);
        }

                // Other drop-in request configurations...

                mPromise = promise;
                dropInClient.launchDropIn(dropInRequest);
            }
        
            @Override
            public void onDropInSuccess(@NonNull DropInResult result) {
                if (mPromise != null) {
                    PaymentMethodNonce paymentMethodNonce = result.getPaymentMethodNonce();
                    String nonce = paymentMethodNonce.getString();
                    boolean threeDSecure = false;
        
                    if (paymentMethodNonce instanceof CardNonce) {
                        CardNonce cardNonce = (CardNonce) paymentMethodNonce;
                        ThreeDSecureInfo threeDSecureInfo = cardNonce.getThreeDSecureInfo();
        
                        if (threeDSecureInfo != null) {
                            threeDSecure = threeDSecureInfo.isLiabilityShifted() || threeDSecureInfo.isLiabilityShiftPossible();
                        }
                    }
        
                    mPromise.resolve(convertNonceToWritableMap(nonce, threeDSecure));
                    mPromise = null;
                }
            }
        
            @Override
            public void onDropInFailure(@NonNull Exception error) {
                if (mPromise != null) {
                    mPromise.reject("DROP_IN_ERROR", error.getMessage());
                    mPromise = null;
                }
            }
        
            @Override
            public void onUserCanceled() {
                if (mPromise != null) {
                    mPromise.reject("USER_CANCELED", "User canceled the payment");
                    mPromise = null;
                }
            }
        
            // Helper method to convert the nonce and threeDSecure to a WritableMap
        
            @NonNull
            private WritableMap convertNonceToWritableMap(String nonce, boolean threeDSecure) {
                WritableMap map = Arguments.createMap();
                map.putString("nonce", nonce);
                map.putBoolean("threeDSecure", threeDSecure);
                return map;
            }
        
            @Override
            public String getName() {
                return "RNBraintreeDropIn";
            }
        }
        

       
