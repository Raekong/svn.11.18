define(
    ["homecommon", "widget","datacenter"],
    function(hc, widget, dc){
        function Profile( container ){
            this.container = container;
            this.widget = new widget();
            this.hc = new hc(container);
            this.hc.init(
                function( menu ){
                }
            );
            var _that = this;

            this.userId = this.container.attr('userid');
            this.lang = this.container.attr('i18n');

            this.init = function(){
                var inputCells = _that.container.find("div[cell]");
                inputCells.each(
                    function(){
                        _that.widget.init($(this), 'input');
                    }
                )

                this.i18n = JSON.parse(this.container.find('#i18n').attr('i18n'));

                $.post(
                    '/user/findById',
                    {  id: _that.userId  },
                    function (res){
                        console.log(res);
                        for(var attr in res){
                            var tag = '[data-'+attr+']';
                            if(_that.container.find(tag).length>0){
                                _that.container.find(tag).val(res[attr])
                                if(attr == 'password'){
                                    _that.container.find('[data-repassword]').val(res[attr])
                                }else if(attr == 'email'){
                                    _that.container.find('[data-reemail]').val(res[attr])
                                }else if(attr == 'country'){
                                    $('ul#countries li').each(function(){
                                        if($(this).attr('data') == res[attr]){
                                            _that.container.find('[data-country]').val($(this).text())
                                            _that.container.find('[data-country]').attr('code', $(this).attr('data'));
                                        }
                                    })
                                }
                            }
                        }
                    }
                )

                this.container.find('[data-country]').attr('readonly', "readonly").click(
                    function(event){
                        $('ul#countries').show();
                        $('ul#countries li').unbind().click(
                            function(){
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
                'data-middlename': {
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
                },'data-interests': {
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
                        _that.widget.msgbox(  _that.lang=='true','alert', _that.i18n.info, _that.i18n.errorformat);
                    }
                    return ;
                }else{  //校验成功
                    var data = data.data;
                    if( data.email != data.reemail ){
                        _that.container.find('[data-reemail]').addClass('error');
                        _that.widget.msgbox( _that.lang=='true','alert', _that.i18n.info, _that.i18n.emailnoequals);
                        return;
                    }
                    if( data.repassword != data.password ){
                        _that.container.find('[data-repassword]').addClass('error');
                        _that.widget.msgbox( _that.lang=='true','alert', _that.i18n.info, _that.i18n.passnoequals);
                        return;
                    }

                    data.userId = _that.userId;
                    data.country = _that.container.find('[data-country]').attr('code');
                    delete data.reemail;
                    delete data.repassword;
                    console.log(data)

                    $.post(
                        '/user/updateInformation',
                        {user: data},
                        function( rst ){
                            _that.widget.msgbox( _that.lang=='true','info', _that.i18n.info, _that.i18n.submitconfirm);
                            return true;
                        }
                    )
                }
            }


        }
        return Profile;
    })