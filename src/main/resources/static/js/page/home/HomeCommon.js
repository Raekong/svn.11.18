define(
    ["template", "popup", "widget", "template"],
    function( tpl, popup, wd, tpl ){
        function HomeCommon( container ){
            this.container = container;
            this.wd = new wd();
            this.init = function( menucallback ){

                var _that = this;
                //绑定下拉菜单
                this.container.find('.headmenus .header').hover(
                    function(){ $(this).find('.downbtn').show() },
                    function(){ $(this).find('.downbtn').hide() },
                );
                this.container.find('.siderswitch').click( //切换边栏收展
                    function(){
                        if( $(this).hasClass('compress')){
                            $('.headlogo').width(228);
                            $('#slogo').hide();
                            $('#publisher').show();

                            $('#menulog').hide();
                            $('#siderbarmenu').show();
                            $('#menus').css('flex-basis', '228px');
                            $(this).removeClass('compress');

                        }else{
                            $('#headlogo').width(48);
                            $('#slogo').css('display', 'inline-block');
                            $('#publisher').hide();

                            $('#menulog').show();
                            $('#siderbarmenu').hide();
                            $('#menus').css('flex-basis', '48px');
                            $(this).addClass('compress');
                        }
                    }
                );
                
                this.container.find('.langswitch').click(
                    function(){
                        var cls = $(this).attr('class');
                        var lang = '';
                        if( cls.indexOf('en') != -1){
                            lang = "zh";
                        }else{
                            lang = "en";
                        }

                        $.post(
                            '/user/i18n',
                            {lang: lang},
                            function( rst ){
                                window.location.reload();
                            }
                        )
                    }
                );
                
                this.container.find('#logout').click(
                    function(){
                        $.post(
                                '/user/logout',
                                function(){
                                    var abbr = $('#frame').attr('abbr');
                                    window.location.href = '/login/' + abbr;
                                }
                        )                        
                    }
                );

                this.container.find('#profile').click(
                    function(){
                         var abbr = $('#frame').attr('abbr');
                         window.location.href = '/user/profile/' + abbr;
                    }
                );
                
                var liTpl = '{{each list as data}}<li class="pageLink" id="{{data.id}}"><a href="{{data.url}}">{{#data.icon}}<span>{{data.text}}</span></a></li>{{/each}}';
                $.post(
                    '/home/menu',
                    function(rst){
                       var list = [];
                       var abbr = rst.abbr;
                       for(var key in rst.menu){
                            var obj = {};
                            obj.text = key;
                            obj.url = rst.menu[key].split(";")[1]  ;
                            obj.icon = rst.menu[key].split(";")[2];
                            obj.id = rst.menu[key].split(";")[0];
                            list.push(obj);
                       }
                       var render = tpl.compile(liTpl);
                       var data = {};
                       data.list = list;
                       var liDom = $(render( data ));
				       _that.container.find('#siderbarmenu').append( liDom );
                       _that.container.find('#siderbarmenu').find('.pageLink').click(
                           function(){
                               window.location.href = $(this).find('a').attr('href');
                           }
                       );
                       menucallback(  _that.container.find('#menus') );

                    }
                );

                _that.selectJournal = function(){
                    $(this).closest('#journals').find('span.checked').removeClass('checked');
                    $(this).addClass('checked');
                }

                _that.container.find('[srctag=journalListSelect]').click(
                    function(){
                        $.post(
                            '/home/submit',
                            function(rst){
                                var lang = rst.lang;
                                var list = rst.list;
                                $.post(
                                    '/home/home-submit-pop/' + lang,
                                    function( rst ){
                                        var dom = $(rst);
                                        var title = dom.find('.title').text();
                                        _that.submitPop = new popup(title, dom,  '668px', function(dom, index){ 
                                            
                                            _that.wd.getTable( )(
                                                dom.find('#journals'), tpl, list, function(){
                                                    dom.find('span.nocheck').click( _that.selectJournal  )
                                                }, 'journalId'
                                            )

                                            dom.find('#cancel').click(
                                                function(){
                                                    _that.submitPop.close();
                                                }
                                            )

                                            dom.find('#save').click(
                                                function(){
                                                    var sele = dom.find('span.checked');
                                                    if( sele.length == 1 ){
                                                        var jid = sele.closest('.row').attr('journalid');  
                                                        window.location.href = '/journal/submit/'+jid;
                                                    }
                                                }
                                            )
                                        });
                                        _that.submitPop.pop();
                                    }
                                )
                                
                            }
                        )
                    }
                )
            }
        }
        return HomeCommon;
    }
)