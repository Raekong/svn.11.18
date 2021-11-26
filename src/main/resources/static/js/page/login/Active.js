define(
    ["widget"],
    function( wd){
        function Active( container ){
            this.container = container;
            this.lang = $('#container').attr('i18n');
            this.i18n = JSON.parse(this.container.find('#i18n').attr('i18n'));
            this.wd = new wd();
            var _that = this;


            this.init = function(){
                this.container.find('#submit').click(
                    function(){
                        $.post(
                            '/user/resendActive',
                            function(rst){
                                if( rst.flag ){
                                    _that.wd.msgbox(
                                        _that.lang == 'true',
                                        'info',
                                        _that.i18n.info,
                                        _that.i18n.resend
                                    )
                                }else{
                                    _that.wd.msgbox(
                                        _that.lang == 'true',
                                        'info',
                                        _that.i18n.info,
                                        _that.i18n.resendFail
                                    )
                                }
                                
                            }
                        )
                    }
                )
            }
        }
        return Active;
    }
)