define(
    ["widget",  "render", "template"],
    function( widget, render, template){
        function Query( container, ec ){
            this.queryConds = null;
            this.container = container;
            this.widget = new widget();
            this.render = new render( container );

            this.name = "Query";
            this.ec = ec;
            this.ec.addHandler( this ); 

            var _that = this;

            this.init = function(){
                this.bindQueryCondition( _that.container.find('#firstquery'));

                this.container.find('#idbut').click(
                    function(){
                        var newQuery = _that.container.find('#queryTemplate').clone(); 
                        newQuery.css('display', 'flex').attr('id', '');
                        _that.bindQueryCondition(newQuery);
                        _that.container.find('#queryTemplate').before(newQuery);

                    }
                )

                this.container.find("#queryStart").click(
                    _that.getParams
                )
            }

            this.query = function( num ){
                //根据当前的查询条件来判断
                if( !!this.queryConds ){
                   //是面板查询
                    this.queryWithConds( num );
                }else{
                    this.queryWithOutConds(num);
                }
            }
            //=====----有条件查询-------------------------------------------------
            //入口可以通过查询面板，触发LIST ADAPT的事件回调过来，也可以是分页页数变化直接查询过来
            this.queryWithConds = function( num ){
                
                _that.widget.getEditorPage()(
                    _that.lang=='true' ? 'zh' : 'en',
                    template,
                    _that.container.find('#editor'),
                    '/article/list/query',
                    {conds : encodeURIComponent(JSON.stringify(_that.queryConds))},
                    num,
                    function(array){
                        _that.render.exec(_that.ec, array, true);
                    },
                    _that.render.editorTableCallback
                );
            }

            //======---收集查询参数-----------------------------------------------
            //判断是否是部分设置，造成参数不完整
            this.isPartNull = function(data){
                var hasValue = false;
                for(var key in data){
                    if( key == 'logic') continue; //前面的条件逻辑本来就一定有值
                    if(!!data[key] ) hasValue = true;
                    else{
                        if(hasValue) return true;
                    }
                }
                return false;
            }

            this.verifyValue = function(data){
                if( data.type == 'int' ){
                    return /^(\-)?[0-9]+$/.test(data.value);
                }
                return true;
            }

            

            //======---分别初始化查询的表，及表中的细节信息--------------------------
            this.initFollowList = function( dom ){
                var callback = dom.attr('callback');
                switch(callback){
                    case 'table':
                        this.initDetail(dom);
                        _that.container.find('.autolist').hide();
                        break;
                    case 'detail':
                        this.initQueryCondition(dom);
                        break;
                }
            }
            //设置查询的具体参数值
            this.initQueryCondition = function( dom ){
                var type = dom.attr('type');
                var intOper = {}; intOper.opers = ["=", "≥", "≤"];
                var dateOper = {}; dateOper.opers = ["on", "start", "end"];
                var stringOper = {}; stringOper.opers = ["like"];
                var status = {}; status.opers = ["equals"];

                status.statusItem = [
                    { t:'Pre-review', s:'1,9'},
                    { t:'Reviewer required', s:'2'},
                    { t:'In Reviewing', s:'3'},
                    { t:'Revision Required', s:'2'},
                    { t:'Change Journal', s:'23'},
                    { t:'Payment Required', s:'17'},
                    { t:'Similarity Check', s:'11'},
                    { t:'Copyediting', s:'20,21'},
                    { t:'Accepted', s:'6'},
                    { t:'Declined', s:'5'}
                ];

                var statusLiTpl = '<div class="autolist" style="z-index: 99; background: #fff;border:  1px solid #c9d8db; text-align: center; height: auto; top:32px; left:0px; display: none;">'
                                + '<ul>{{each statusItem as item}} <li status="{{item.s}}">{{item.t}}</li> {{/each}}</ul>';
                var statusDom = template.compile(statusLiTpl)(status);

                var checkRst = {};
                checkRst.rstItem = [
                    {t: 'Pass', s: '1'},{t: 'Fail', s: '0'}
                ]
                var checkRstTpl = '<div class="autolist" style="z-index: 99; background: #fff;border:  1px solid #c9d8db; text-align: center; height: auto; top:32px; left:0px; display: none;">'
                + '<ul>{{each rstItem as item}} <li status="{{item.s}}">{{item.t}}</li> {{/each}}</ul>';
                var checkDom = template.compile(checkRstTpl)(checkRst);

                var reviewRst = {};
                reviewRst.rstItem = [
                    {t: 'Revision', s: 'revision'},{t: 'Accept', s: 'accept'},{t: 'Decline', s: 'decline'}
                ]
                var reviewRstTpl = '<div class="autolist" style="z-index: 99; background: #fff;border:  1px solid #c9d8db; text-align: center; height: auto; top:32px; left:0px; display: none;">'
                + '<ul>{{each rstItem as item}} <li status="{{item.s}}">{{item.t}}</li> {{/each}}</ul>';
                var reviewRstDom = template.compile(reviewRstTpl)(reviewRst);


                var tpl = '{{each opers as oper}} <li oper="{{oper}}">{{oper}}</li> {{/each}}';
                var operContianer = dom.closest('.querycondition').find('.queryOper');
                var paramContainer = dom.closest('.querycondition').find('.params');

                paramContainer.empty().attr('type', type);
                paramContainer.append("<input class='queryparam'>");
                switch(type){
                    case 'int':
                        var lis = template.compile(tpl)(intOper);
                        operContianer.find(".query").text("=");
                        paramContainer.find('input').focus();
                        break;
                    case 'checkresult':
                        operContianer.find(".query").text("equals");
                        paramContainer.append(checkDom);
                        paramContainer.find('input').click(
                            function(){
                                paramContainer.find('.autolist').show();
                            }
                        );
                        break;
                    case 'status':
                        operContianer.find(".query").text("equals");
                        paramContainer.append(statusDom);
                        paramContainer.find('input').click(
                            function(){
                                paramContainer.find('.autolist').show();
                            }
                        );
                        break;
                    case 'reviewresult':
                        operContianer.find(".query").text("equals");
                        paramContainer.append(reviewRstDom);
                        paramContainer.find('input').click(
                            function(){
                                paramContainer.find('.autolist').show();
                            }
                        );
                        break;
                    case 'string':
                        operContianer.find(".query").text("like");
                        var lis = template.compile(tpl)(stringOper);
                        break;
                    case 'date':
                        paramContainer.find('input').attr('readonly', 'readonly').each(
                            function(){
                                _that.widget.datepicker(this, 'en');
                            }
                        )
                        operContianer.find(".query").text("on");
                        var lis = template.compile(tpl)(dateOper);;
                        break;
                }
                
                operContianer.find(".autolist ul").empty().append(lis);
                _that.bindQueryCondition(dom.closest('.querycondition'));
            }

            //填充下一级下拉列表
            this.filterDetail = function(detail, table){
                var rst = [];
                var queryDetail = [];
                for(var key in detail){
                    queryDetail.push( detail[key]);
                }

                queryDetail.sort(
                    function(d1, d2){
                        return d1.index - d2.index;
                    }
                )

                queryDetail.forEach(
                    function(detail){
                        if( !!detail.query ){
                            detail.query.forEach(
                                function( t ){
                                    var data = t.split("|");
                                    rst.push({ 'key': data[0], 'type': data[1]})
                                }
                            )
                        }
                    }
                )
                //console.log(rst);
                return rst;            
            }

            this.initDetail = function( dom ){
                var table = dom.attr('data');
                $.post(
                    '/article/list/querySetting/'+table,
                    function(rst){ 
                        var detailLists = _that.filterDetail(rst, table);
                        var followContainer = dom.closest('.querycondition').find('.detail .autolist ul');
                        followContainer.empty();
                        dom.closest('.querycondition').find('.detail .query').text('').attr('data', '');
                        dom.closest('.querycondition').find('.value div').empty();
                        dom.closest('.querycondition').find('[param="queryOper"]').empty();
                        dom.closest('.querycondition').find('[param="value"]').empty();

                        var detailTpl = '<li data="{{key}}" type="{{type}}" callback="detail">{{key}}</li>';
                        var render = template.compile(detailTpl);
                        detailLists.forEach(
                            function(d){
                                followContainer.append( render(d) );
                                _that.bindQueryCondition(dom.closest('.querycondition'));
                            }
                        )
                    }
                )
            }

            //=====----------整理查询参数
            this.getParams = function(){   
                var t = [];
                _that.container.find('.querycondition:not("#queryTemplate")').each(
                    function(){
                        var tmp = {};
                        $(this).find("[param]").removeClass('error').each(
                            function(){
                                if( !!$(this).attr('type') ){
                                    tmp[ $(this).attr('param') ] = $(this).find('input').val();
                                    tmp.type = $(this).attr('type');
                                }else{
                                    tmp[ $(this).attr('param') ] = $(this).text();
                                }
                            }
                        );
                        if( _that.isPartNull(tmp) ){ //只设置了部分参数，提示补齐
                            $(this).find("[param], .params").addClass('error');
                            return;
                        }else if( !tmp.value ){ //如果是全空就丢掉这一行
                            return;
                        }else if(  !_that.verifyValue(tmp) ){
                            $(this).find('.params').addClass('error');
                            return;
                        }else{
                            t.push(tmp);   
                        }             
                    }
                )
                //console.log(t);
                _that.queryConds = t;
                if( !!t ){
                    _that.ec.fire(
                        _that.name,
                        'queryWithCond',
                        null
                    );                    
                }
                
            }

            //======--------初始化面板事件--------------------------
            this.bindQueryCondition = function( dom ){
                dom.find('.query').unbind().click(
                    function(){
                        $(this).parent().find('.autolist').show();
                    }
                )

                //=======----------点击LI触发的事件------------------
                dom.find('.autolist li').unbind().click(
                    function(event){
                        //专用为状态参数设定准备的
                        if( $(this).attr('status') ){
                            $(this).closest('.params').find('input').val( $(this).text());
                            $(this).closest('.params').find('.autolist').hide();
                            event.stopPropagation();    
                            return;
                        }

                        $(this).closest('.droplist').find('.query').text($(this).text()).attr('data', $(this).attr('data'));
                        if(!!$(this).attr('callback')){
                            _that.initFollowList($(this));
                        }
                        $(this).closest('.droplist').find('.autolist').hide();
                        event.stopPropagation();    
                    }
                )
                
                //=======---------删除一行的触发事件------------------
                dom.find('.del').unbind().click(
                    function(){
                        $(this).closest('.querycondition').remove();
                    }
                )

            }

            //=====无条件查询------------------------------------------
            this.queryWithOutConds = function(num ){
                _that.widget.getEditorPage()(
                    _that.lang=='true' ? 'zh' : 'en',
                    template,
                    _that.container.find('#editor'),
                    '/article/editor/list',
                    {},
                    num,
                    function(array){
                        _that.render.exec(_that.ec, array);
                    },
                    _that.render.editorTableCallback
                );
            }


        }
        return Query;
    }
)