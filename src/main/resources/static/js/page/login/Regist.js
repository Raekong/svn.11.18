define(
    ["widget","datacenter"],
    function(widget, dc){
        function Regist( container ){
            this.container = container;
            this.widget = new widget();
            this.isZH = ($('#container').attr('i18n') == 'true');
            var _that = this;

            this.init = function(){
                var inputCells = _that.container.find("div[cell]");
                inputCells.each(
                    function(){
                        _that.widget.init($(this), 'input');
                    }
                )

                this.i18n = JSON.parse(this.container.find('#i18n').attr('i18n'));

                this.container.find('[data-country]').attr('readonly', "readonly").click(
                    function(event){
                        $('ul#countries').show();
                        $('ul#countries li').unbind().click(
                            function(  ){
                                console.log($(this).text());
                                _that.container.find('[data-country]').val($(this).text());
                                _that.container.find('[data-country]').attr('code', $(this).attr('data'));
                                $('ul#countries').hide();
                            }
                        )
			            event.stopPropagation();    
                    }
                );

                this.container.click(
                    function(){
                        $('ul#countries').hide();
                    }
                )

                this.container.find('#submit').click(
                    _that.submit
                )
            }

            
                
            this.ds = {
                'data-firstname': {
                    'verify': 'nonull',
                    'val' : ''
                },
                'data-midname': {
                    'verify': '',
                    'val' : ''
                },
                'data-lastname': {
                    'verify': '',
                    'val' : ''
                },
                'data-affiliation': {
                    'verify': 'nonull',
                    'val' : ''
                },
                'data-country': {
                    'verify': 'nonull',
                    'val' : ''
                },
                'data-email': {
                    'verify': 'email',
                    'val' : ''
                },
                'data-reemail': {
                    'verify': 'email',
                    'val' : ''
                },
                
                'data-password': {
                    'verify': 'nonull',
                    'val' : ''
                },
                'data-repassword': {    
                    'verify': 'nonull',
                    'val' : ''
                },'data-interest': {
                    'verify': 'nonull',
                    'val' : ''
                }
            }

            this.dc = new dc( this.container, this.ds );
            this.submit = function(){
                $('.error').removeClass('error');
                var data = _that.dc.exec();
                
                if( !!data.verify.key ){ //校验失败
                    var dom =  data.verify.dom;
                    dom.addClass('error');    
                    
                    if( dom.parent().attr('data').indexOf('email') != -1 ){
                        _that.widget.msgbox(_that.isZH, 'alert', _that.i18n.info, _that.i18n.errorformat );
                    }
                    return ;
                }else{  //校验成功
                    var data = data.data;
                    if( data.email != data.reemail ){
                        _that.container.find('[data-reemail]').addClass('error');
                        _that.widget.msgbox(_that.isZH, 'alert', _that.i18n.info, _that.i18n.emailnoequals );
                        return;
                    }
                    if( data.repassword != data.password ){
                        _that.container.find('[data-repassword]').addClass('error');
                        _that.widget.msgbox(_that.isZH, 'alert', _that.i18n.info, _that.i18n.passnoequals );
                        return;
                    }


                    data.country = _that.container.find('[data-country]').attr('code');
                    data.abbr = $('#container').attr("abbr");
                    $.post(
                        '/user/regist',
                        { user: data },
                        function( rst ){
                            if( rst.userId != -1){
                                _that.widget.msgbox(_that.isZH, 'info', _that.i18n.info, _that.i18n.submitconfirm,function(){
                                    window.location.href = '/login/' + _that.container.attr('abbr');
                                });
                            }else{
                                _that.widget.msgbox(_that.isZH, 'alert',_that.i18n.info , _that.i18n.existemail);
                            }
                            
                            return true;
                        }
                    )
                }
            }


        }
        return Regist;
})