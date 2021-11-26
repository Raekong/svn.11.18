define(
    ["homecommon", "widget", "datacenter", "template"],
    function( hc, widget, dc, tpl ){
        function List(  container ){
            this.container = container;
            this.hc = new hc(container);
            this.hc.init(
                function( menu ){
                    menu.find('li#2').addClass('current');
                }
            );

            this.widget = new widget();
            this.lang = $('#frame').attr('i18n');

            var _that = this;

            this.disablePublisher = function(  ){
                var pid = $(this).closest('.row').find('[id]').attr('id');
                $.post(
                    '/publisher/disable',
                    {id : pid},
                    function(){
                        _that.widget.msgbox(this.lang=='true', 'info', _that.i18n.info, _that.i18n.dsiable,  function(){
                        window.location.reload();
                    });
                       
                    }
                )
            }

            this.activePublisher = function(  ){
                var pid = $(this).closest('.row').find('[id]').attr('id');
                $.post(
                    '/publisher/active',
                    {id : pid},
                    function(){
                        _that.widget.msgbox(this.lang=='true', 'info', _that.i18n.info, _that.i18n.enable,  function(){
                            window.location.reload();
                        });
                        
                    }
                )
            }

            this.configPublisher = function(  ){
                var pid = $(this).closest('.row').find('[id]').attr('id');
                window.location.href = '/publisher/config/' + pid
            }

            this.init = function(){
                var active = ["正常", "active"];
                var disable = ["禁用", "disable"];

                var name = this.container.find('input#name').val();
                var abbr = this.container.find('input#abbr').val();
               
                this.widget.getPage()(
                    'zh', tpl, this.container.find('#testtable'),
                    '/publisher/list', {name:name, abbr:abbr} , 10, 
                    function( array ){
                        array.forEach(
                            function(d){
                                if( d.disable ){
                                    d.disable = disable[_that.lang == 'en'? 1 : 0];
                                }else{
                                    d.disable = active[_that.lang == 'en'? 1 : 0];
                                }
                            }
                        )
                    },
                    function(container){
                        container.find('.disable').click(
                            _that.disablePublisher
                        );

                        container.find('.active').click(
                            _that.activePublisher
                        );

                        container.find('.config').click(
                            _that.configPublisher
                        )
                    }
                );

                this.container.find('#query').click(
                    function(){
                        _that.container.find('#queryForm').submit();
                    }
                );

                //国标化
                _that.i18n = JSON.parse( _that.container.find('#i18nInfos').attr('i18n') );
            }
        }

        return List;
    }
)