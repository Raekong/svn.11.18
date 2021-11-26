define(
    ["widget", "datacenter", "editor"],
    function( wd, dc, ed){
        function WorkFlow(container){
            this.container = container;
            var _that = this;
            this.lang = $('#frame').attr('i18n');
            this.wd = new wd();
            this.wd.init( this.container.find('#workflow-config'), 'tab');

            this.init = function(){
                
                this.initPreCheck();
                this.loadTechCheck();
                this.initSimilarCheck();

                this.container.find('[submit]').click(
                    function(){
                        var point = $(this).attr('submit');
                        switch( point ){
                            case 'Technical Check':
                                _that.submitPreCheck();
                                break;
                            case 'workflow':
                                _that.submitSimilarCheck();
                        }
                    }
                );
                this.loadWorkFlowDefine();

            }

            
            this.loadWorkFlowDefine = function(){
                var configPoints = [
                    "Payment", "Simaliary Check First",
                    "Simaliary Check", "Total Similar", "First Similar", "Second Similar", "Third Similar",
                    "Similarity Revision Due"
                ];
                
                configPoints.forEach(
                    function(c){
                        $.post(
                            '/journal/getWorkFlowDefine',
                            {configPoint: c},
                            function( rst ){
                                _that.fillConfigPoint(c, rst.configContent);
                            }
                        )
                    }
                )
            }

            var loaded = 0;

            this.loadFinal = function(){
                loaded += 1;
                if( loaded < 7) return; //加载选项之后，再查看是否选中选项，决定是否隐藏某些配置项
                else{
                    setTimeout(
                        function(){
                            if( _that.container.find('[configPoint="Simaliary Check"]').hasClass('checked')){
                                _that.container.find('#similarvalues').css('display', 'inline-block');
                            }else{
                                _that.container.find('#similarvalues input').val('');
                            };
                        },200
                    )
                    
                }
            }

            this.fillConfigPoint = function( cp, data ){
                
                switch( cp ){
                    case 'Simaliary Check First':
                    case 'Simaliary Check':
                        if(data == 'true'){
                            _that.container.find('[configPoint="'+ cp +'"]').addClass('checked');
                        }
                        break;
                    case 'Total Similar':
                    case 'Similarity Revision Due':
                    case 'First Similar':
                    //case 'Second Similar':
                    //case 'Third Similar':
                        _that.container.find('[configPoint="'+ cp +'"]').val(data);
                        break;
                }
                _that.loadFinal();
            }

            //==================预审配置-------------------------------------------
            this.initPreCheck = function(){
                this.container.find('[configPoint="Technical Check"]').click(
                    function(){
                        if($(this).hasClass('checked')){
                            $(this).removeClass("checked");
                        }else{
                            var dom = $(this);
                            dom.addClass('checked');
                        }
                    }
                )
            }
            this.loadTechCheck = function(){
                $.post(
                    '/journal/getAllSettingByJid',
                    function(rst){
                        rst = JSON.parse(rst);
                        rst.forEach(
                            function(c){
                                if( c.configPoint == "Technical Check"){
                                    if( c.configContent == 'true'){
                                         _that.container.find('[configpoint="Technical Check"]').click();
                                     }
                                }
                            }
                        );
                    }
                )    
            }
            this.submitPreCheck = function(){
                var data = {};
                data['Technical Check'] = _that.container.find('[configpoint="Technical Check"]').hasClass('checked');
                
                for(var key in data){
                    var tmp = {};
                    tmp.configPoint = key;
                    tmp.configContent = data[key];
                    $.post(
                        '/journal/save',
                        tmp,
                        function(rst){
                            
                        }
                    )
                }
                _that.wd.msgbox(
                    _that.lang == 'true',
                    'info',
                    'Message',
                    '配置项设置成功'
                )
            }
            

            //==================查重配置-------------------------------------------
            this.initSimilarCheck = function(){
                this.container.find(' [configpoint="Simaliary Check First"],[configpoint="Simaliary Check"]').click(
                    function(){
                        if($(this).hasClass('checked')){
                            $(this).removeClass("checked");
                        }else{
                            $(this).addClass("checked");
                        }
                        //查重检测面板
                        if($(this).attr('configpoint') =="Simaliary Check"){
                            if($(this).hasClass('checked')){
                                _that.container.find('#similarvalues').css('display', 'inline-block');
                            }else{
                                _that.container.find('[configpoint="Simaliary Check First"]').removeClass('checked');
                                _that.container.find('#similarvalues').css('display', 'none');
                            }
                        }
                        if($(this).attr('configpoint') =="Simaliary Check First"){
                            if($(this).hasClass('checked')){
                                _that.container.find('[configpoint="Simaliary Check"]').click();
                            }
                        }
                    }
                )
            }

            this.submitSimilarCheck = function(){
                var data = {};
                data['Simaliary Check'] = _that.container.find('[configpoint="Simaliary Check"]').hasClass('checked');
                data['Simaliary Check First'] = _that.container.find('[configpoint="Simaliary Check First"]').hasClass('checked');
                data['Total Similar'] = _that.container.find('[configpoint="Total Similar"]').val();
                data['First Similar'] = _that.container.find('[configpoint="First Similar"]').val();
                //data['Second Similar'] = _that.container.find('[configpoint="Second Similar"]').val();
                //data['Third Similar'] = _that.container.find('[configpoint="Third Similar"]').val();
                data['Similarity Revision Due'] = _that.container.find('[configPoint="Similarity Revision Due"]').val();
                
                for(var key in data){
                    var tmp = {};
                    tmp.configPoint = key;
                    tmp.configContent = data[key];

                    $.post(
                        '/journal/save',
                        tmp,
                        function(rst){
                            
                        }
                    )
                }
                _that.wd.msgbox(
                    _that.lang == 'true',
                    'info',
                    'Message',
                    '配置项设置成功'
                )
            }

        }
        return WorkFlow;
    }
)