define(
    ["homecommon", "widget", "datacenter"],
    function( hc, widget, dc ){
        function Create(  container ){
            this.container = container;
            this.hc = new hc(container);

            var _that = this;
            this.hc.init(
                function( menu ){
                    if( _that.container.find("#config").length > 0){ //为修改配置准备
                        menu.find('li#2').addClass('current');
                    }else
                        menu.find('li#1').addClass('current');
                }
            );

            this.widget = new widget();
            this.lang = this.container.attr('lang')
            
            this.init = function(){
                var inputCells = _that.container.find("div[cell]");
                inputCells.each(
                    function(){
                        _that.widget.init($(this), 'input');
                    }
                )

                $.post(
                    '/component/modules/'+this.lang,
                    function(rst){
                        _that.container.find('#modules').append(rst);
                        _that.container.find('.server').click(
                            function(){
                                if($(this).hasClass('checked')){
                                    $(this).removeClass('checked');
                                }else{
                                    $(this).addClass('checked');
                                }
                        });
                        _that.container.find('[server=PAPERPROCESS]').addClass('checked').unbind();
                        _that.configInit(); //如果是配置，则启动配置
                    } 
                )

                _that.widget.init(this.container.find('#tabcontainer'), 'tab');

                _that.container.find('.lang').click(
                    function(){
                        if($(this).hasClass('checked')){
                            return;
                        }else{
                            _that.container.find('.lang').removeClass('checked');
                            $(this).addClass('checked');
                            _that.container.find('#lang').attr('data-lang', $(this).attr('lang'));
                        }
                    }
                );

                

                _that.container.find('#address').text('');

                _that.container.find('#submit').click(
                    _that.submit
                )

                //国标化
                _that.i18n = JSON.parse( _that.container.find('#i18nInfos').attr('i18n') );
               
            }

            this.fillPayments = function( ps ){
                for(var k in ps ){
                    var settings = ps[k];
                    $('#payselect span[data='+ k +']').addClass('checked');
                    $('#payment div.' + k + '>div').each(
                        function(){
                            var key = $(this).attr('data');
                            $(this).find('input').val(settings[key]);
                        }
                    )
                }
            }

            this.configInit = function(){
                if( _that.container.find("#config").length > 0){ //为修改配置准备
                    var p = JSON.parse(_that.container.find("#config").text());
                    
                    _that.container.attr("data-id", p.id);
                    _that.container.find("[data-name]").val(p.name);
                    _that.container.find("[data-abbr]").val(p.abbr).attr('disabled','disabled').css('background', '#eee');
                    _that.container.find("[data-host]").val(p.host);
                    _that.container.find("[data-port]").val(p.port);
                    _that.container.find("[data-password]").val(p.password);
                    _that.container.find("[data-email]").val(p.emailAddress);
                    _that.container.find("[data-emailsender]").val(p.emailSender);
                    _that.container.find("div#address").html(p.contact);
                    _that.container.find("[server]").removeClass('checked');
                    p.modules.forEach(
                        function( d ){
                            _that.container.find("[server=" + d + "]").addClass('checked');
                        }
                    );

                    for(var i=0; i<p.modules.length; i++){
                        if( p.modules[i] == 'PAYMENT'){
                            _that.fillPayments(JSON.parse(p.paymentSetting));
                        }
                    };

                    console.log(p);
                    _that.container.find("[lang]").removeClass('checked');
                    _that.container.find("[lang="+ p.i18n +"]").addClass('checked');
    
                    delete _that.ds['data-rootemail'];
                    delete _that.ds['data-rootpassword'];
                    delete _that.ds['data-rootname'];
                }
            }

           
            this.ds = {
                'data-name': { 
                    'verify': 'nonull',
                    'val' : ''
                },
                'data-abbr':{
                    'verify': 'nonull',
                    'val' : ''
                },
                'data-lang':{
                    'verify': 'nonull',
                    'val' : ''
                },
                'data-host':{
                    'verify': 'nonull',
                    'val' : ''
                },
                'data-port':{
                    'verify': 'int',
                    'val' : ''
                },
                'data-password':{
                    'verify': 'nonull',
                    'val' : ''
                },
                'data-email':{
                    'verify': 'email',
                    'val' : ''
                },
                'data-emailsender':{
                    'verify': 'nonull',
                    'val' : ''
                },
                'data-rootemail':{
                    'verify': 'email',
                    'val' : ''
                },
                'data-rootpassword':{
                    'verify': '',
                    'val' : ''
                },
                'data-rootname':{
                    'verify': 'nonull',
                    'val' : ''
                }
            }

            this.dc = new dc( container, this.ds );
            
            this.submit = function(){
                var data = _that.dc.exec();
                _that.container.find('.error').removeClass('error');

                if(!!data.verify.key){
                    data.verify.dom.addClass('error');
                }else{
                    
                    

                    if( !!_that.container.find( '#address' ).text().trim() ){
                        var spans = _that.container.find('.mtitle span.nocheck');
                        var modules = [];
                        spans.each(
                            function(){
                                if($(this).hasClass('checked')){
                                    modules.push($(this).attr('server'));
                                }
                            }
                        )
                        data.data.modules = JSON.stringify(modules);
                        data.data.address = _that.container.find( '#address' ).html();
                        data.data.address = data.data.address.replace(/<\/(p|div)>/ig,"<br />").replace(/<(?!br\b)\/?\w+[^>]*>/ig,"");

                        data.data.paymentSetting = '{}';
                        //为支付做准备，如果需要支付付费
                        if( _that.container.find( '[server="PAYMENT"]').hasClass('checked')){
                            var params = {};
                            var flag = false;
                            $('#payselect span[data].checked').each(
                                function(){
                                    var payselect = $(this).attr('data');
                                    var paramboard = $('#payment .'+ payselect + ' input');                                
                                    params[payselect] = {};
                                    paramboard.each(
                                        function(){
                                            if( flag ) return;
                                            var key = $(this).closest('div').attr('data');
                                            var param =  $(this).val();
                                            if( !param ){
                                                $(this).addClass('error');
                                                _that.widget.msgbox(_that.lang!='en', 'alert', 'Payment Setting', 'The Payment Setting can\'t be setting to null!');
                                                flag = true;
                                                return;
                                            }
                                            params[payselect][key] = $(this).val();
                                        }
                                    );
                                }
                            )

                            if( flag ) return;
                            data.data.paymentSetting = JSON.stringify(params);
                        };

                        
                        var url = '/publisher/regist';
                        var msg = {
                            title: _that.i18n.info, 
                            content: !!_that.container.attr("data-id") ?  _that.i18n.submitconfirm : _that.i18n.confirmnew,
                            but: _that.i18n.confirm };

                        if( _that.container.find("#config").length > 0){//为修改配置准备
                            data.data.id = _that.container.attr("data-id");
                            url = '/publisher/update'   
                        };
                        //console.log(data.data);
                        
                        $.post(
                            url,
                            data.data,
                            function( rst ){
                                if( !!rst.id ){
                                    _that.widget.msgbox(_that.lang!='en', 'info', msg.title, msg.content);
                                }
                            }
                        );
                    }else{
                        _that.container.find( '#address' ).css('border', '1px solid #C8252C');
                    }
                   

                    console.log(data);
                }
            }


        }

        return Create;
    }

)