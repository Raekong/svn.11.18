define(
    ["eventcenter", "datacenter"],
    //预加载STRIPE
    function( ec,  dc){
        function Login( container ){
            this.container = container;
            this.abbr = container.attr('abbr');
            this.enterSignIn = function(event){
				var e = event;
				var code = e.keyCode ? e.keyCode : e.which ? e.which : e.charCode;
				if (13 == code) {  
					event.data.handler.signIn(event);
				} 
			}

            this.signIn = function(event){
				var login = event.data.handler;
                var data = login.dc.exec();
                
                if( !!data.verify.key ){ //校验失败
                    var dom =  data.verify.dom;
                    dom.addClass('error');
                    if(data.verify.key == 'data-email'){
                        _that.container.find('div.error').text(_that.i18n.emailfail);
                    }else if(data.verify.key == 'data-password'){
                        _that.container.find('div.error').text(_that.i18n.passwordfail);
                    }
                }else{  //校验成功
                    data.data.abbr = _that.abbr;
                    console.log( data.data );
                    $.post(
                        '/login/do',
                        data.data,
                        function( rst ){
                            console.log(rst);
                            if( rst.flag ){
                                if( !!rst.info ){
                                    window.location.href = '/login/active/' + _that.abbr + "?email=" + data.data.email;
                                }else
                                    window.location.href = '/home/' + _that.abbr ;
                            }else{
                                if( rst.banned ){ 
                                    _that.container.find('div.error').text(_that.i18n['banned']);
                                }else{
                                    _that.container.find('div.error').text(_that.i18n['login-info']);
                                }
                                
                            }
                        }
                    )
                }
			}
			
			this.keyUp = function(event){
				event.data.handler.container.find('div.error').html('&nbsp;');	//清理上一次登录时出错标记
                event.data.handler.container.find('.input').addClass('error');
			}

            var _that = this;
            this.init = function(){
                _that.i18n = JSON.parse($('#i18nInfos').attr('infos'));
                
            }			
			
            this.eventHandlers =  {
				"click" : {
					"signin" : this.signIn
				},
				"blur": {
					"input": this.keyUp
				},
				"keypress":{
					"enter" : this.enterSignIn
				}
			}


            this.ds = {
                'data-email': { 
                    'verify': 'email',
                    'val' : ''
                },

                'data-password':{
                    'verify': 'nonull',
                    'val' : ''
                }
            }

            this.ec = new ec();
            this.ec.addHandler( this );

            this.dc = new dc( container, this.ds );
            this.ec.registEventBind( this );
        }
        return Login
    }
)