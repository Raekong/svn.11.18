define(
    ["template", "widget", "popup", "editor"],
    function( template,wd, popup, ed){
        function Email(container){
            this.container = container;
            this.lang = $('#frame').attr('i18n');
            this.configTpl = '<div style="cursor:pointer;" class="configpoint" data="{{configPoint}}" id="{{id}}" email="{{email ? \"true\" : \"false\"}}"><div class="h4title"><span>{{configPoint}}</span></div></div>';

            this.wd = new wd();
            
            var _that = this;
            this.tpls = null;

            this.init = function(){
                $.post(
                    '/journal/emailconfig/all',
                    function(rst){
                        _that.tpls = rst;
                        var render = template.compile(_that.configTpl);
                        _that.container.find('#tpllist').empty();
                        for(var i=0; i<rst.length; i++){
                            var dom = render(rst[i]);
                            _that.container.find('#tpllist').append(dom);
                        }
                       
                        _that.container.find('.configpoint').click(
                            _that.select
                        )

                        _that.container.find('.configpoint').eq(0).click();
                    }
                )
            }

            this.select = function(){
                _that.container.find('.select').removeClass('select');
                $(this).addClass('select');
                _that.load(
                    $(this).attr('data')
                )

                window.scrollTo(0, 80);
            }

            this.configContentTpl = '<div  class="configpoint" data="{{configPoint}}" id="{{id}}" email="{{email ? \"true\" : \"false\"}}"><div class="h4title"  >{{configPoint}} </div>'
            + '<div style="padding: 8px; border: 1px solid #c9d8db; ">'
            + '<label>Send Message with Email ? <span class="nocheck {{email ? \"checked\" : \"\"}}" sendwithemail ></span> </label>'
            + '</div><div class="h4title">Templates</div>'
            + '   <div class="tablehead">'
            + '     <div  class="cell" style="flex: 1 1 65%">Name</div>'
            + '     <div  class="cell" style="flex: 1 1 15%">Is Default</div>'
            + '     <div  class="cell" style="flex: 1 1 20%">Action</div>'
            + '   </div>'
            + '{{each tpls as tpl }}'
            +' <div class="row" id="{{tpl.id}}" data="{{configPoint}}" name="{{tpl.name}}" email="{{email ? \"true\" : \"false\"}}">'
            +'      <div class="tplName" style="flex: 1 1 65%">{{tpl.name}}</div>'
            +'      <div class="isDefault" style="flex: 1 1 15%"><span class="{{ tpl.defaultTpl ? \"nocheck checked\" : \"nocheck\" }}"></span></div>'
            +'      <div class="operate"style="flex: 1 1 20%"><span class="copy"><i class="fa fa-files-o" ></i></span>{{ if tpl.id != 0}}<span class="edit"><i class="fa fa-pencil" ></i></span> <span class="del"><i class="fa fa-trash" ></i></span>{{/if}}</span></div>'                            
            + '</div>{{/each}}'
            +'</div>';

            this.load = function( data ){
                var tpl = null;
                _that.tpls.forEach(element => {
                    if( element.configPoint == data) tpl = element;
                });
                console.log(tpl);
                if( tpl != null ){
                    var render = template.compile(_that.configContentTpl);
                    _that.container.find('#tplcontent').empty().append(render(tpl));
                    _that.container.find('#tplcontent').find('[sendwithemail]').click(
                        _that.setEmail
                    );
                    _that.container.find('#tplcontent').find('.row div.isDefault span').click(
                        _that.setDefault
                    );

                    _that.container.find('#tplcontent').find('.row span.del').click(
                        _that.delTpl
                    );

                    _that.container.find('#tplcontent').find('.row span.copy').click(
                        _that.copy
                    );

                    _that.container.find('#tplcontent').find('.row span.edit').click(
                        _that.edit
                    );
                } 
            }

            this.edit = function(){
                var id = $(this).closest('.row').attr('id');
                var tpl = $(this).closest('.row');
                var point = $(this).closest('.row').attr('data');
                _that.container.find('#emailtpl').remove();
                $.post(
                    '/journal/emailtpl-pop',
                    function( rst ){
                        var dom = $(rst);
                        dom.attr('configPoint', point);
                        dom.insertAfter(tpl);
                        window.scrollTo(0, 80);
                        _that.ed = new ed('messageedit', this.lang=='true', 288);
                        _that.ed.init();
                        _that.fillTpl( id, dom );

                        dom.find('#save').click(
                            function(){
                                _that.saveTpl(dom);
                            }
                        )
                    }
                )
            }

            this.saveTpl = function( dom ){
                var data = {}; 
                data.name = dom.find('[data-name]').val();
                data.title = dom.find('[data-title]').val(  );
                
                data.content = _that.ed.html(  );
                data.id = dom.find('#emailtpl').attr('data');
                data.recevier = -1;
                if( dom.find('#recevsetting').is(':visible') ){
                    data.recevier = dom.find('#recevsetting span.checked').attr('data');
                }else{
                    data.recevier = 0;
                }

                var point = dom.attr("configPoint");
                $.post(
                    '/journal/setting/email/update',
                    data,
                    function(rst){
                        _that.reloadTpls(point);
                    }
                )
            }

            this.fillTpl = function(id, dom){
                $.post(
                    '/journal/setting/email/getById',
                    {tid: id},
                    function( rst ){
                        dom.find('#emailtpl').attr('data', id);

                        var title = '';
                        if( !!rst.titleCH && !!rst.titleEN ){ title = ( _that.lang == 'true' ? rst.titleCH : rst.titleEN ); };
                        title = rst.titleCH + rst.titleEN;

                        var content = '';
                        if( !!rst.tplCH && !!rst.tplEN ){ content = ( _that.lang == 'true' ? rst.tplZH : rst.tplEN ) };
                        content = rst.tplZH + rst.tplEN;

                        dom.find('[data-name]').val(rst.name);
                        dom.find('[data-title]').val( title );
                        _that.ed.set( content );
                        
                        if( rst.recipient != 0){ 
                            dom.find('span.nocheck[data='+rst.recipient+']').addClass('checked');
                        }else{//表示是系统默认，不可选
                            dom.find('#recevsetting').hide();
                        }

                        dom.find('span.nocheck').click(
                            function(){
                                dom.find('span.nocheck').removeClass('checked');
                                $(this).addClass('checked');
                            }
                        )
                    }
                )
            }

            this.copy = function(){
                var id = $(this).closest('.row').attr('id');
                var point = $(this).closest('.row').attr('data');
                var name = $(this).closest('.row').attr('name');
                var email = $(this).closest('.row').attr('email');
                $.post(
                    '/journal/setting/email/copy',
                    {id: id, configPoint: point, name: name, email: email=='true' },
                    function( rst ){
                        _that.reloadTpls(point);
                    }
                )
            }

            this.reloadTpls = function( configPoint ){
                $.post(
                    '/journal/emailconfig/all',
                    function(rst){
                        _that.tpls = rst;
                        _that.load( configPoint );
                });
            }

            this.delTpl = function(){
                var id = $(this).closest('.row').attr('id');
                var configPoint = $(this).closest('.row').attr('data');//tpl id
                
                $.post(
                    '/journal/setting/email/del',
                    {id: id},
                    function( rst ){
                        _that.reloadTpls(configPoint);
                    }
                )
            }

            this.setDefault = function(){
                var id = $(this).closest('.row').attr('id');//tpl id
                var configPoint = $(this).closest('.row').attr('data');//tpl id
                $.post(
                    '/journal/setting/email/default',
                    {id: id},
                    function( rst ){
                        _that.wd.msgbox(_that.lang == 'true', 'info', "Set Default Template", "The Configuration has srtted successfull.", function(){
                            _that.reloadTpls(configPoint);
                        });
                        
                    }
                )
            }

            this.setEmail = function(){
                var id = $(this).closest('.configpoint').attr('id');
                var point = $(this).closest('.configpoint').attr('data');
                var flag = null;
                if( $(this).hasClass('checked') ){
                    flag = false;
                    $(this).removeClass('checked');
                }else{
                    flag = true;
                    $(this).addClass('checked');
                }
                $.post(
                    '/journal/setting/email/setWithEmail',
                    {id: id, configPoint: point, flag: flag},
                    function(rst){
                        _that.wd.msgbox(_that.lang == 'true', 'info', "Send Email", "The Configuration has srtted successfull.");
                    }
                )
            }
        }
        return Email;
    }
)