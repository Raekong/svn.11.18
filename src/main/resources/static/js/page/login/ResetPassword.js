define(
    ["eventcenter", "datacenter"],
    function( ec,  dc){
        function Resetpassword(container){
            this.container = container
            this.abbr = container.attr('abbr');

            this.resetPassword = function(event){
                var login = event.data.handler;
                var data = login.dc.exec();

                if( !!data.verify.key ){ //校验失败
                    var dom =  data.verify.dom;
                    dom.addClass('error');
                    if(data.verify.key == 'data-email'){
                        _that.container.find('div.error').text(_that.i18n.emailfail);
                    }
                }else{  
                    var params = {};
                    params.email = data.data.email;
                    params.pulisherAbbr = _that.abbr;

                    $.post(
                        '/user/resetpassword/do',
                        params,
                        function( rst ){
                            console.log(rst)
                            if(rst.flag){
                                _that.container.find('.error').text( _that.i18n.sended );
                            }else{
                                _that.container.find('.error').text( _that.i18n.sendedfail );
                            }
                        }
                    )
                }
            }

            var _that = this;
            this.init = function(){
                _that.i18n = JSON.parse($('#i18nInfos').attr('infos'));
            }

            this.eventHandlers =  {
                "click" : {
                    "reset" : this.resetPassword
                }
            }

            this.ds = {
                'data-email': {
                    'verify': 'email',
                    'val' : ''
                }
            }

            this.ec = new ec();
            this.ec.addHandler( this );
            this.dc = new dc( container, this.ds );
            this.ec.registEventBind( this );
        }
        return Resetpassword
    }
)