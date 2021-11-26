define(
    ["template", "popup"],
    function( template, popup ){
        function Render( container){
            this.container = container;

            var _that = this;
            _that.currentListData = null;
           
            this.reorginzeData = function(data, list){
                data.forEach(
                    function(d){
                    }
                )
            }

            
             this.showDetailInfos = function(aid){
                $.post(
                    '/article/listboard/showDetailInfos/'+aid,
                    function(rst){
                        var render = template.compile(rst);
                        var data = null;
                        for(var i=0; i<_that.currentListData.length; i++){
                            if(_that.currentListData[i].aid ==  aid ){
                                data = _that.currentListData[i];
                            }
                        }

                        rst = render( data );
                        var dom = $(rst);
                        var title = dom.find('.title').text();
                        _that.msgpop = new popup(title, dom,  ['868px'], function(dom, index){
                            dom.find('#cancel').click(
                                function(){
                                    _that.msgpop.close();
                                }
                            );
                        });
                        _that.msgpop.pop();
                    }
                )
            }

            //从数据中抽取出确定的值
            this.tractValue = function(part, data , obj ){
                if( !!obj ){
                    if(part == 'article'){
                        console.log(obj);
                        if( data == 'id')
                            return obj[data].aid ;
                        if( data == 'title')
                            return obj[data].title ;
                        if( data = 'js');
                           
                    }

                    return obj;
                }else{
                    return '-';
                }
            }

            this.editorTableCallback = function( dom, data ){
                var cells = _that.container.find('#editor .table_detail');
                data.forEach(
                    function(d){
                        if( !!d.review.lastRound ){
                            d.review.lastRound.reviewers = JSON.parse(d.review.lastRound.reviewers);
                        }
                        var row = $('<div class="artilce_row"><div style=" width:88px; margin-right: 8px; ;display:inline-block;"><span class="widow"></span></div></div>');
                        var headpart = $('<div class="headPart"></div>');
                        var curPart = '';
                        cells.each(
                            function(){
                                var part = $(this).attr('part');
                                if(part != curPart){ //新的类型来了
                                    if( curPart != ''){
                                        row.append(headpart);
                                    }
                                    headpart = $('<div class="headPart"></div>');
                                    curPart = part;
                                }
                                var data = $(this).attr('data');
                                var cls = $(this).attr('class');
                                var cell = $('<div>').attr('part', part).attr('data', data).attr('class',cls).text(_that.tractValue(part, data, d[part]));
                                headpart.append( cell );
                            }
                        );

                        row.append(headpart);
                        dom.append(row);
                    }
                )
                dom.find('.row span.widow').click(
                    function(){
                        var id = $(this).closest('.row').attr('id');
                        window.open('/article/'+id);  
                    }
                )

                dom.find('.row span.view').click(
                    function(){
                        var id = $(this).closest('.row').attr('id');
                        _that.showDetailInfos(id);
                    }
                )
            }

            //==---------按后台返回的数据，进行渲染-----------------------------------
            this.getHeadSetting = function(setting, array ){
                var dataShowConfig = [];
                for(var key in array){
                    var set = setting[key];
                    var showPart = [];
                    for( var data in array[key]){
                        set.forEach(
                            function(x){
                                if( x[0] == data){
                                    showPart.push({key:x[0], title: x[1], index: x[3], size:x[4]});
                                }
                            }
                        )
                    }
                    showPart.sort(
                        function(a,b){
                            return (a.index - b.index);
                    });
                    dataShowConfig.push({ part: key, setting: showPart} );
                }
                return dataShowConfig ;
            }

            this.getPartName = function( part ){
                var rst = '';
                switch( part ){
                    case 'article':
                        rst = 'Article Informations';
                        break;
                    case 'review':
                        rst = 'Review';
                        break;
                    case 'similar':
                        rst = 'Similarity Check';
                        break;
                    case 'payment':
                        rst = 'Payment';
                        break;
                    case 'copyedit':
                        rst = 'Copyedit';
                        break;
                }
                return rst;
            }

            this.constructTableHead = function( partName, partSetting ){
                var part = '<div class="headPart"><div class="part"></div><div class="details"></div></div>';
                var details = '<div part="'+partName+'"></div>';
                var partDom = $(part);
                
                //这是得到一个表头
                partDom.find('.part').text(this.getPartName(partName));
                partSetting.forEach(
                    function( setting ){
                        var dom = $(details);
                        dom.attr('class', 'table_detail ' + setting.size);
                        dom.attr('data', setting.key);
                        dom.text(setting.title);

                        partDom.find('.details').append(dom);
                    }
                );

                return partDom;
            }

            this.buildTableHead = function(head, setting, array ){
                //得到一个返回的行数据，用这个数据来构建表头
                var headSet = this.getHeadSetting(setting, array[0]);
                
                var heads = {};
                headSet.forEach(
                    function(x){
                        heads[x.part] = _that.constructTableHead( x.part, x.setting );
                    }
                );

                var partIndex = ['article', 'review', 'similar', 'payment', 'copyedit'];
                
                partIndex.forEach(
                    function( type ){
                        if( !!heads[type]){
                            head.append(heads[type]);
                        }
                    }
                )
            }

            this.exec = function(ec, array, isWithCond ){
                //拿到系统的默认配置
                var sysSetting = ec.fire(
                    'Render',
                    'sysShowConfig',
                    null                
                )[0].rst;

                if( array.length == 0 && !isWithCond ){ //只有一开始打开的时间才要隐藏
                    _that.container.find('li[data=editor]').addClass('empty').hide(); //关闭没有论文的卡片页
                    _that.container.find('.tabDiv[tag=editor]').hide();
                }  

                //再根据后台查询返回的数据，对要显示的数据进行过滤
                if( array.length > 0 ){
                    //先得到表头，我们要把表头先放到页面中去，然后显示就直接显示就可以了
                    //所以表头是关键
                    var head = _that.container.find('#editor .tablehead');
                    head.append('<div class="headPart " style=" width:88px;" >Action</div>');
                    
                    this.buildTableHead(head, sysSetting, array );
                    head.find('.table_detail').each(function(index, value){ 
                        if( index % 2 == 1){
                            $(value).addClass('even');
                        }
                    })
                    _that.container.find('li[data=editor]').attr('loaded', 'true'); //加载成功
                }
                
            }

            

        }
        return Render;
    }
)                                                         