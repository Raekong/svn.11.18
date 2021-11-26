define(
    ["mystripe","eventcenter","uploader", "popup", "dataverifier"],
    function(mystripe,ec,uploader, popup, dv){
        function PaymentCompent( container ){
            this.name = "paymentcomponent";
            this.container = container;
            this.ec = new ec('debug');
            this.ec.addHandler( this ); 

            var _that = this;
            this.init = function(){
                this.container.find('#aid').keyup(
                    _that.loadArticleInfo
                )

                this.container.find('[type="wire"]').click(
                    _that.uploadRecipt
                )

                this.container.find('[type="stripe"]').click(
                    _that.online
                )
            }

            this.online = function(){
                var data = {};

                _that.container.find('.error').removeClass('error');
                var email = $('#email').val();
                if( !dv.verifyEmail(email)){
                    $('#email').addClass('error');
                    return;
                }

                var aid = $('#aid').val();
                if( !dv.isInt(aid)){
                    $('#aid').addClass('error');
                    return;
                }

                var total = $('#total').val();
                if( !dv.isInt(total)){
                    $('#total').addClass('error');
                    return;
                }

                data.email = email;
                data.aid = aid;
                data.type = 'online';
                data.total = total;
                $.post(
                    '/payment/stripe/back',
                    data,
                    function( rst ){
                        var dom = $(rst);
                        var title = 'Stripe Payment';
                        _that.pop = new popup(title, dom,  '688px', function(dom, index){
                            var stripe = new mystripe(dom, _that.onlinepaid);
                            stripe.init();
                            dom.find('#cancel').click(
                                function(){
                                    _that.pop.close();
                                }
                            )
                        });
                        _that.pop.pop();
                    }
                )
            }

            this.onlinepaid = function(s, card){
                s.createToken(card).then(function (result) {
                    if (result.error) {
                        // Inform the user if there was an error.
                        var errorElement = document.getElementById('card-errors');
                        errorElement.textContent = result.error.message;
                    } else {
                        // Send the token to your server.
                        var token = result.token.id;
                        var email = $('#email').val();
                        var aid = $('#aid').val();
                        var total = $('#totalamount').val();
                        $.post(
                            "/create-charge-back",
                            {email: email, aid: aid, total: total, token: token},
                            function (data) {
                                if(data == "true"){
                                    $('#note').text('APC has paid by stripe successfully, waiting for audit by financial editor. ');
                                    _that.clear();
                                    _that.pop.close();
                                }else{
                                    document.getElementById('card-errors').textContent = 'Stripe payment failed, please check your credit card and account.';
                                }
                            });
                    }
                });
            }

            this.uploadRecipt = function(){
                _that.container.find('.error').removeClass('error');
                var email = $('#email').val();
                if( !dv.verifyEmail(email)){
                    $('#email').addClass('error');
                    return;
                }

                var aid = $('#aid').val();
                if( !dv.isInt(aid)){
                    $('#aid').addClass('error');
                    return;
                }

                var total = $('#total').val();
                if( !dv.isInt(total)){
                    $('#total').addClass('error');
                    return;
                }

                $.post(
                    '/submit/submit-upload-pop/' + (this.lang ? 'zh' : 'en'),
                    function(rst){
                        var dom = $(rst);
                        var title = 'Upload Bank Recipet Slip';
                        
                        _that.pop = new popup(title, dom,  '688px', function(dom, index){
                            dom.find('.title span').text(title);
                            dom.find('#fileTypeLabel').text('Recipet File');

                            //去掉其它可选项
                            dom.find('.autolist li:not(:contains('+ fileType +'))').remove();
                            dom.find('#fileType').val('Bank Recipet Slip').attr('readonly', 'readonly');
                            //去掉文件列表
                            dom.find('#uploadfiles').remove();

                            var uploader = _that.initUploader(dom.find('#uploadDiv'), _that.ec, null, false);
                            uploader.init();

                            dom.find('#cancel').click(
                                function(){
                                    _that.pop.close();
                                }
                            )
                        });
                        _that.pop.pop();
                    }
                )
            }

            this.loadArticleInfo = function(){
                var aid = $(this).val();
                if(!dv.isInt(aid)){
                    $(this).addClass('error');
                }else{
                    $.post(
                        '/payment/comp/'+aid,
                        function(rst){
                            if( !!rst['#Article Title#'] ){
                                _that.fillArticleInfo(rst);
                            }else{
                                //不能支付
                                $('#note').text('The journal to which this article belongs is not configured for APC, or the article is in a state where it cannot receive APC.');
                                return;
                            }
                            
                        }
                    )
                }
            }

            this.initUploader = function(dom, ec, uploadBut, lang){
                var up = new uploader(
                    dom, 
                    ec,
                    $('#uploadConfirm'),  //确定上传按键
                    lang
                );
                return up;
            }

            this.fillArticleInfo = function(rst){
                _that.container.find('#atitle').text( rst['#Article Title#']);
                _that.container.find('#authors').text(rst['#Contributor Name#']);
                _that.container.find('#journal').text(rst['#Journal Title#']);   
                $('#note').text('');
            }

            
            this.payedWrie = function(file){
                var data = {};
                
                data.paytotal = $('#total').val();
                data.email = $('#email').val();
                data.type = 'wire';
                data.isBack = true;
                data.aid = $('#aid').val();
                //防止用户因为没有填写邮箱,被阻止后,重新提交,但没有上传材料文件
                if(!!file.originName){
                    _that.wiredata = file;
                }
                data = $.extend(data, _that.wiredata);

                $.post(
                    '/payment/wire/back',
                    data,
                    function( rst ){
                        $('#note').text('Bank receipt has been uploaded successfully, waiting for audit by financial editor. ');
                        _that.clear();
                        _that.pop.close();
                    }
                )
            } 

            this.clear = function(){
                $('#email, #aid, #total, #email').val('');
                $('#atitle, #authors, #journal').text('');
            }

            this.exports = {
                'uploaded' : this.payedWrie
            }

        }

        return PaymentCompent;
    }
)