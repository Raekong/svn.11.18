define(
    ["paginate", 'myLayer', "template"],
    function(paginate, layer, ltpl){
        function Widget(  ){

            var msgboxTpl = '<div class="msgbox">'
                            +   '<div class="msgboxtitle"><span class="{{type}}"></span><span>{{title}}</span></div>'
                            +   '<div class="msgboxcontent">{{content}}</div>'
                            +   '<div class="msgboxfoot"><div class="confirm">{{confirm}}</div></div>'
                            +'</div>';
            
            var confirm = ['确定','Confirm'];
            this.msgbox = function(i18n, type, title, content, callback){
                var data = {};
                data.type = type;
                data.title = title;
                data.content = content;
                data.confirm = i18n ? confirm[0] : confirm[1];

                var dom = ltpl.compile(msgboxTpl)(data);
                layer.layer.open({
                    title: false,
                    closeBtn: 1,
                    offset: '80px',
                    type: 1,
                    content: dom,
                    area: '368px',
                    success: function(layero, index){
                        layero.find('.confirm') .click(
                            function(){
                                if(!!callback){
                                    callback();
                                    layer.layer.close(index);
                                } else{
                                    layer.layer.close(index);
                                }
                                
                            }
                        )
                    }
                });
            }

            

            this.datepicker = function(inputId , lang, cb){
                layer.date.render({
                    elem: inputId,
                    lang: lang,
                    done: cb
                });
            }


            this.autoComplete = function(
                container, 
                url, 
                tpl, 
                datacallback,
                rendCallback,
                itemClickCB
            ){  //自助完成的，三部分组成 .list, 放内容，内容每条是 .item ,最终的输入框为 .input
                //要放置的内容，定义在每个ITEM的data属性中
                var isArray = function isArray(obj){
                    return (typeof obj=='object')&&obj.constructor==Array;
                }
                var update = function(){
                    
                    var key = container.find('.autoinput').attr('param');
                    var value = container.find('.autoinput').val();
                    var data = {};
                    data[key] = value.trim();
                    $.post(
                        url,
                        data,
                        function( rst ){
                            container.find('.autoinput').attr('data', '');
                            var resultData = null
                            if( !isArray(rst) ){ resultData = JSON.parse(rst); }
                            else{
                                resultData = rst;
                            }
                            
                            if( resultData.length == 0 ){
                                container.find('.autolist').hide();
                                return;
                            }
                            var list = datacallback(resultData);
                            var data = {}; data.list = list;
                            var render = ltpl.compile( tpl );
                            var dom = render( data );
                            container.find('.autolist').empty().append(dom).show().find('.autoitem').click(
                                function(e){
                                    container.find('.autoinput').val( $(this).text()).attr('data', $(this).attr('data'));
                                    container.find('.autolist').hide();
                                    itemClickCB($(this));
                                    e.stopPropagation();    
                                }
                            );

                            $(window).click(
                                function(){
                                    container.find('.autolist').hide();
                                }
                            )

                            rendCallback( container.find('.list') );
                        }
                    )
                }
                container.find('.autoinput').keyup(
                    function(){
                        if(!!container.find('.autoinput').val( ) ){
                            update();
                        }
                    }                   
                );
                
            }

            function Tab(container){
                var con = container;
                var tabs = con.find('li.mytab a');
                console.log(tabs.length);
                tabs.each(
                    function(){
                        $(this).unbind();
                        $(this).click(
                            function( event ){
                                //console.log($(this).html());
                                $(this).closest('.nav').find('li.mytab a').removeClass('active');
                                //container.find('div[tag=' + targetDiv + ']').siblings().hide();
                                con.find('>div.tabDiv').hide();
                                var targetDiv = $(this).attr('tag');
                                con.find('div[tag=' + targetDiv + ']').show();
                                $(this).addClass('active');
                                event.stopPropagation();    
                            }                            
                        )
                    }
                )
                tabs.eq(0).click();
            }

            function Input( container ){
                var pre = container.attr('cell');
                var data = container.attr('data');
                var required = container.attr('required') != undefined;
                
                container.css('flex', '1 1 ' + pre );
                var label = $('<label>').html(  container.attr('title') + (required? '<span style="color:red;">*</span>':''));
                var input = $('<input>').attr('data-'+data, '');
                container.append(label).append(input);
            }

            //简单表格的控件,CONTAINER中接收二个DOM，一个是HEAD, 一个是BODY，
            //HEAD中每个单元格有class= cell, 且有CELL属性，指定比例，还有data属性，指定存放的数据项
            //如果没有CELL，则直接复制到ROW的行中
            // 单元格cell，data属性表示要渲染的数据项，const表示渲染为填充固定不变的内容
            function Table( container, template, array, callback, expendData ){
                var tpl = "{{ each list as data }}<div class='row' ";
                
                var keys = "";
                if( !!expendData ){
                    expendData.split(',').forEach(
                        function(e){
                            keys += e + "={{data." + e + "}} ";
                        }
                    )
                }
                keys += '>'

                tpl += keys;

                var headCells = container.find('.tablehead .cell');
                headCells.each(
                    function(){
                        var pre = $(this).attr('cell');
                        if( !pre ){
                            tpl += $(this).html();
                            return;
                        }

                        var item = $(this).attr('data');
                        var con = '';
                        var params = '';
                        var last = '';
                        if( !!item ){
                            con = '{{data.'+ item +'}}';
                            params = item +"='{{ data."+item+"}}' ";
                        }else{
                            con = $(this).attr('const');
                        }   
                        tpl += '<div '+params+' style="flex: 1 1 ' + pre + '">'+ con +'</div>';
                    }
                )
                
                tpl += "</div>{{/each}}";
                var data = {};
                data.list = array;
                //console.log(tpl);
                var render = template.compile(tpl);
                var liDom = $(render( data ));
                // console.log(liDom.html());
                container.find('.body').empty().append(liDom);
                callback(container.find('.body'));
            }

            /**
             * 专为编辑文章列表做的分页器
             */
            function EditorParperListPagedTable(lang, template, container,  url, params, pageSize, dataCallback, tableCallback ){
                var pageNum = (lang == 'zh' ? '当前页' : 'Page:');
                var totalPage = (lang == 'zh' ? '总页数' : 'Total Papers:');

                container.find('.body').empty();
                var getPageinateParam = function( url, params, pageSize ){
                    var paramstr = '';
                    
                    for( var key in params ){
                        paramstr += key + "=" +params[key] + '&';
                    }				
                    
                    if( paramstr.endsWith('&') ) paramstr = paramstr.substring(0, paramstr.length-1);
                    var dataSourceUrl = url + "?" + paramstr;
                    
                    var paginateParam = {
                        dataSource: dataSourceUrl,
                        locator: 'content',
                        totalNumberLocator: function(response) {
                            return response.totalElements;
                        },
                        pageSize: pageSize,
                        showNavigator: true,
                        formatNavigator: pageNum +' <span style="color:#f00"><%= currentPage %></span>  of  <%= totalPage %>  ,   '+ totalPage +'  <%= totalPage %>',
                        prevText : '<i class="fa fa-long-arrow-left"></i>',
                        nextText : '<i class="fa fa-long-arrow-right"></i>',
                        ajax: { },
                        callback:  function(data, pg){ 
                            
                            if(!!dataCallback)  dataCallback(data);
                            container.find('.body').empty();
                            tableCallback(container.find('.body'), data);
                        }
                    };
                    return paginateParam;
                }

                container.find('.page').pagination( 
                    getPageinateParam(url, params, pageSize)
                );
                
            }

            //分页表格的控件,CONTAINER中接收三个DOM，一个是分页的表脚 .page，一个是HEAD, 一个是BODY，
            //HEAD中每个单元格有class= cell, 且有CELL属性，指定比例，还有data属性，指定存放的数据项
            //如果没有CELL，则直接复制到ROW的行中
            /**
             * 
             * @param {*} lang : 语种
             * @param {*} container : 总容器
             * @param {*} template : ARTTEMPLATE对象
             * @param {*} array : 服务器返回的分页数据
             * @param {*} url : 服务器提供数据访问的接口
             * @param {*} params : 查询数据参数
             * @param {*} pageSize : 分页大小
             */
            function PagedTable(lang, template, container,  url, params, pageSize, dataCallback, tableCallback, expendData ){
                var pageNum = (lang == 'zh' ? '当前页' : 'Page:');
                var totalPage = (lang == 'zh' ? '总页数' : 'Total Papers:');

                container.find('.body').empty();
                var getPageinateParam = function( url, params, pageSize ){
                    var paramstr = '';
                    
                    for( var key in params ){
                        paramstr += key + "=" +params[key] + '&';
                    }				
                    
                    if( paramstr.endsWith('&') ) paramstr = paramstr.substring(0, paramstr.length-1);
                    var dataSourceUrl = url + "?" + paramstr;
                    
                    var paginateParam = {
                        dataSource: dataSourceUrl,
                        locator: 'content',
                        totalNumberLocator: function(response) {
                            return response.totalElements;
                        },
                        pageSize: pageSize,
                        showNavigator: true,
                        formatNavigator: pageNum +' <span style="color:#f00"><%= currentPage %></span>  of  <%= totalPage %>  ,   '+ totalPage +'  <%= totalPage %>',
                        prevText : '<i class="fa fa-long-arrow-left"></i>',
                        nextText : '<i class="fa fa-long-arrow-right"></i>',
                        ajax: { },
                        callback:  function(data, pg){ 
                            if(!!dataCallback)  dataCallback(data);
                            console.log(data);
                            container.find('.body').empty();
                            var table = new Table( container, template, data, tableCallback, expendData );
                        }
                    };
                    return paginateParam;
                }

                container.find('.page').pagination( 
                    getPageinateParam(url, params, pageSize)
                 );
                
            }

            this.init = function( container , type ){
                var rst = null;
                switch(type){
                    case 'tab':
                        rst = new Tab(container);
                        break;
                    case 'input':
                        rst = new Input(container);
                        break;
                }
                return rst;
            }

            this.getTable = function( ){return Table;}
            this.getPage = function( ){return PagedTable;}
            this.getEditorPage = function( ){return EditorParperListPagedTable;}
            this.msg = function(){
                return MessageBox
            }
      
        }
        return Widget;
    }
)