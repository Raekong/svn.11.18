define(
    ["homecommon", "widget",  "datacenter", "template", "popup"],
    function( hc, widget, dc, tpl, popup ){
        function List( container ){
            this.container = container;
            this.hc = new hc(container);
            this.hc.init(
                function( menu ){
                    menu.find('li#5').addClass('current');
                }
            );
            this.widget = new widget();
            
            this.ds = {
                
                'data-name':{
                    'verify': 'nonull',
                    'val' : ''
                },'data-abbr':{
                    'verify': 'nonull',
                    'val' : ''
                },
                'data-manageemail': { 
                    'verify': 'email',
                    'val' : ''
                }
            }

            var _that = this;


            this.popupCB = function(dom, index){
                dom.find('#save').click(
                    function(){
                        dom.find('.error').removeClass('error');
                        var t = new dc(dom, _that.ds);
                        var data = t.exec();
                        if( !!data.verify.key ){
                            data.verify.dom.addClass('error');
                        }else{
                            var i18n = JSON.parse(dom.find('[i18n]').attr('i18n'));
                            $.post(
                                '/journal/create/do',
                                data.data,
                                function(rst){
                                    if( rst.flag != 'success'){
                                        dom.find('.errorinfo').text(i18n[rst.flag]);
                                    }else{
                                        window.location.reload();
                                    }
                                }
                            )
                        }
                    }
                );

                dom.find("#cancel").click(
                    function(){
                        _that.pop.close();
                    }
                )
            }

            this.create = function(){
                $.post(
                    '/journal/create',
                    function(rst){
                        var dom = $(rst);
                        var inputCells = dom.find("div[cell]");
                        inputCells.each(
                            function(){
                                _that.widget.init($(this), 'input');
                            }
                        );

                        var title = dom.find('.title').text();
                        _that.pop = new popup(title, dom,  ['548px'], _that.popupCB);
                        _that.pop.pop();
                    }
                )
            }

            this.changeOrder = function( dom, dir ){
                var dom1 = null;
                var dom2 = null;
                var neworder = 0;
                
                if( dir == 'up'){
                    dom1 = dom.closest('.row').prev();
                }else{
                    dom1 = dom.closest('.row').next();
                }
               
                if( dom1.length > 0 ){
                    if( dir == 'up'){
                        dom2 = dom1.prev();
                    }else{
                        dom2 = dom1.next();
                    }

                    if( dom2.length > 0 ){
                        neworder = (dom1.attr('jorder')*1.0 + dom2.attr('jorder')*1.0)  / 2;
                    }else{
                        if(dir == 'up') neworder = dom1.attr('jorder') / 2;
                        else neworder = dom1.attr('jorder')*1.0 + 1;
                    }
                }

                if( neworder > 0){
                    var jid = dom.closest('.row').attr('journalid');
                    $.post(
                        '/journal/chanageOrder',
                        {jid: jid, order: neworder},
                        function( rst ){
                            window.location.reload();
                        }
                    )
                }
            }

            this.init = function(){
                this.container.find('#create').click(
                    this.create
                );
                
                var configed = ["配置完成", "Config Completed"];
                var unconfiged = ["配置未完成", "Config Uncompleted"];

                $.post(  
                    '/journal/list/query',
                    function(rst){
                        var flag = $("#frame").attr('i18n');
                        rst.forEach(function(e){
                            if(e.complete) e.status = configed[flag =='true' ? 0: 1];
                            else e.status = unconfiged[flag =='true'? 0: 1];
                        })
                        new _that.widget.getTable()( _that.container.find('.jlist'), tpl, rst, function( dom ){
                            dom.find('.config').click(function(){
                                var id = $(this).closest('.row').find('[journalid]').attr('journalid');
                                window.location.href = '/journal/config/' + id;
                            });

                            dom.find('.up').click(
                                function(){
                                     _that.changeOrder($(this), 'up');
                                }   
                            )

                            dom.find('.down').click(
                                function(){
                                    _that.changeOrder($(this), 'down');
                                }    
                            )
                        },  'jorder,journalId');
                    }
                )
            }
        }

        return List;
    }
)