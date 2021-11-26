define(["dataverifier"], 
    function(dv) {
        function MyStripe(dom , callback){
            this.dom = dom;
            this.callback = callback;
            this.stripeJs = 'https://js.stripe.com/v3/?noext';

            var _that = this;

            this.init = function(){
                $.getScript(this.stripeJs, this.loadStripe);
            }

            this.loadStripe = function(){
                var stripe = window.Stripe;
                if( !!stripe ){
                    var API_KEY = _that.dom.find("#api-key").val();
                    // Create a Stripe client.
                    var s = stripe(API_KEY);
                    // Create an instance of Elements.
                    var elements = s.elements();

                    // Create an instance of the card Element.
                    var card = elements.create('card');

                    // Add an instance of the card Element into the `card-element` <div>.
                    card.mount('#card-element');
                    card.on("ready", function(){
                        $('#loading').hide();
                    })
                   
                    // Handle real-time validation errors from the card Element.
                    card.addEventListener('change', function (event) {
                        var displayError = document.getElementById('card-errors');
                        if (event.error) {
                            displayError.textContent = event.error.message;
                        } else {
                            displayError.textContent = '';
                        }
                    });

                    _that.dom.find('#save').click(
                        function(){
                            $('#loading').show();
                            if( !!_that.callback ){
                                _that.callback( s, card ); //专门为补付提供的接口
                                return;
                            }
                            _that.handlePayments( s, card );
                        }
                    )
                   
                }
            }

            this.handlePayments = function(s, card ){
                s.createToken(card).then(function (result) {
                    if (result.error) {
                        // Inform the user if there was an error.
                        var errorElement = document.getElementById('card-errors');
                        errorElement.textContent = result.error.message;
                    } else {
                        // Send the token to your server.
                        var token = result.token.id;
                        var email = $('#email').val();
                        if(!email && !dv.verifyEmail(email)){
                            document.getElementById('card-errors').textContent = 'Please enter a valid email address.';
                            return;
                        }
                        var aid = $('#aid').val();
                        var pnum = $('#pnum').val();
                        var total = $('#totalamount').val();
                        var payid = $('#payid').val();
                        $.post(
                            "/create-charge",
                            {email: email, aid: aid, total: total, token: token, payid:payid, pnum:pnum},
                            function (data) {
                                if(data == "true"){
                                    window.location.reload();
                                }else{
                                    document.getElementById('card-errors').textContent = 'Stripe payment failed, please check your credit card and account.';
                                }
                            });
                    }
                });
            }
        }
        return MyStripe;
    }
);