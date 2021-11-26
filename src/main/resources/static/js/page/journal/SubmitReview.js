define(
    ["editor", "widget"],
    function( editor, wd ){
        function SubmitReview( container ){
            this.container = container;
            this.lang = $('#frame').attr('i18n');
            this.wd = new wd();
    
            var _that = this;

            this.wd.init( this.container.find('#submittabs'), 'tab');
            

            this.isInt = function(value){
                return /^(\-)?[0-9]+$/.test(value);
            }
            this.saveConfig = function(data){
                $.post(
                    '/journal/save',
                    data,
                    function( rst ){
                       
                    }
                )
            }

            this.submitConfig = function(){
                var point = $(this).attr('configPoint');
                var data = {};
                switch(point){
                   case 'Submit Guidelines':
                        data.configPoint = point;
                        data.configContent = _that.ed1.html();
                        _that.saveConfig(data);
                        _that.wd.msgbox(_that.lang=='true', "info", "Message", "配置项提交成功！");
                        break;
                    case 'Submit':
                        //收集REVIEWERS 要求的配置点
                        //其实是要不要提供审稿人
                        var reviewer = _that.container.find('[configPoint="Submit Requirements"]').hasClass('checked');
                        if( reviewer ){
                            data.configPoint = 'Submit Requirements';
                            data.configContent = 'true';
                            _that.saveConfig(data);

                            data.configPoint = "Require Reviewers Num";
                            data.configContent = _that.container.find('#ReviewersNum').val(  );
                            if(!data.configContent || !_that.isInt(data.configContent)){
                                _that.wd.msgbox(_that.lang=='true', "alert", "消息", "请检查输入数据，应该为整数！");
                                return;
                            }
                            _that.saveConfig(data);

                            data.configPoint = "Reviewer Requirement";
                            data.configContent = _that.ed2.html();
                            _that.saveConfig(data);

                        }else{
                            data.configPoint = 'Submit Requirements';
                            data.configContent = 'false';
                            _that.saveConfig(data);

                            data.configPoint = "Require Reviewers Num";
                            data.configContent = "0";
                            _that.saveConfig(data);

                            data.configPoint = "Reviewer Requirement";
                            data.configContent = '';
                            _that.saveConfig(data);
                        }
                        //收集COVER LETTER 声明要求的配置点
                        var flag = _that.container.find('[configPoint="CoverLetter Check"]').hasClass('checked');
                        data.configPoint = "CoverLetter Check";
                        if( flag )
                            data.configContent = 'true';
                        else
                            data.configContent = 'false';
                        _that.saveConfig(data);
                        
                            //收集COVER REQUIREMENT 声明要求的配置点        
                        data = {};
                        data.configPoint = "CoverLetter Requirement";
                        if( flag ){//如果COVER声明要检查，则一定要提交COVER LETTER
                            data.configContent = 'true';
                        }else{
                            flag = _that.container.find('[configPoint="CoverLetter Requirement"]').hasClass('checked');
                            if( flag ){
                                data.configContent = 'true';
                            }else{
                                data.configContent = 'false';
                            }
                        }
                        _that.saveConfig(data);

                        //收集LATEXS 要求的配置点
                        flag = _that.container.find('[configPoint="Latex Check"]').hasClass('checked');
                        data.configPoint = "Latex Check";
                        if( flag )
                            data.configContent = 'true';
                        else
                            data.configContent = 'false';
                        _that.saveConfig(data);
                        _that.wd.msgbox(_that.lang=='true', "info", "消息", "配置项提交成功！");
                        break;

                    
                    case "Review Settings":
                        //审稿的三个期限点
                        var responsedue = _that.container.find('[data-responsedue]').val();
                        var reviewdue = _that.container.find('[data-reviewdue]').val();
                        var revisiondue = _that.container.find('[data-revisiondue]').val(); 

                        if( !responsedue || !_that.isInt(responsedue)
                            || !reviewdue || !_that.isInt(reviewdue) 
                            || !revisiondue || !_that.isInt(revisiondue)
                        ) {
                            _that.wd.msgbox(_that.lang=='true', "alert", "消息", "请检查输入数据，应该为整数！");
                            return;
                        }

                        data.configPoint = "Review Due";
                        data.configContent = reviewdue;
                        _that.saveConfig(data);

                        data.configPoint = "Response Due";
                        data.configContent = responsedue;
                        _that.saveConfig(data);

                        data.configPoint = "Revision Due";
                        data.configContent = revisiondue;
                        _that.saveConfig(data);
                        //审稿打分这个功能取消掉
                        // flag = _that.container.find('[configPoint="Review Score Module"]').hasClass('checked');
                        // data.configPoint = "Review Score Module";
                        // data.configContent = flag ? 'true' : 'false';
                        // _that.saveConfig(data);

                        //审稿ACTION的配置点
                        //是否有拒稿通知
                        flag = _that.container.find('[configPoint="Review Decline Notify"]').hasClass('checked');
                        data.configPoint = "Review Decline Notify";
                        if( flag )
                            data.configContent = 'true';
                        else
                            data.configContent = 'false';
                        _that.saveConfig(data);

                        //是否达到指定数量，邮件通知
                        flag = _that.container.find('[configPoint="Review Finish Num Notify"]').hasClass('checked');
                        data.configPoint = "Review Finish Num Notify";
                        if( flag ){
                            data.configContent = 'true';
                            var num = _that.container.find('[configPoint="Review Result Num"]').val();
                            if( !num || !_that.isInt(num) ){
                                _that.wd.msgbox(_that.lang=='true', "alert", "消息", "请检查输入数据，应该为整数！");
                                return;
                            }else{
                                //保存指定数量值
                                _that.saveConfig(data);
                                data.configPoint = "Review Result Num";
                                data.configContent = num;
                                _that.saveConfig(data);
                            }
                        }else{
                            data.configContent = 'false';
                            _that.saveConfig(data);
                            data.configPoint = "Review Result Num";
                            data.configContent = '';
                            _that.saveConfig(data);
                        }

                        //是否逾期提醒
                        flag = _that.container.find('[configPoint="Review OverDue Notify"]').hasClass('checked');
                        data.configPoint = "Review OverDue Notify";
                        if( flag ){
                            data.configContent = 'true';
                            var times = _that.container.find('[configPoint="Review OverDue Notify Time"]').val();
                            var pre = _that.container.find('[configPoint="Review OverDue Notify Period"]').val();
                            if( !times || !_that.isInt(times) || !pre || !_that.isInt(pre) ){
                                _that.wd.msgbox(_that.lang=='true', "alert", "消息", "请检查输入数据，应该为整数！");
                                return;
                            }else{
                                //保存指定数量值
                                _that.saveConfig(data);
                                data.configPoint = "Review OverDue Notify Time";
                                data.configContent = times;
                                _that.saveConfig(data);
                                data.configPoint = "Review OverDue Notify Period";
                                data.configContent = pre;
                                _that.saveConfig(data);
                            }
                        }else{
                            data.configContent = 'false';
                            _that.saveConfig(data);

                            data.configPoint = "Review OverDue Notify Time";
                            data.configContent = '';
                            _that.saveConfig(data);
                            data.configPoint = "Review OverDue Notify Period";
                            data.configContent = '';
                            _that.saveConfig(data);
                        }
                        _that.wd.msgbox(_that.lang=='true', "info", "消息", "配置项提交成功！");
                        break;
                    
                }
                
            }

            this.init = function(){
                this.ed1 = new editor('author-guidelines', _that.lang == 'zh', 208);
                this.ed1.init();

                this.ed2 = new editor('reviewer-requirement', _that.lang == 'zh', 108);
                this.ed2.init();

                this.container.find('[littlebut]').click(
                    _that.submitConfig
                );

                this.container.find('[configPoint="Submit Requirements"]').click(
                    function(){
                        if($(this).hasClass('checked')){
                            _that.container.find('#ReviewersNum').hide();
                            _that.container.find('#reviewer-requirement').hide();
                            $(this).removeClass('checked');
                        }else{
                            _that.container.find('#ReviewersNum').show();
                            _that.container.find('#reviewer-requirement').show();
                            $(this).addClass('checked');
                        }
                    }
                )

                this.container.find(
                    '[configPoint="CoverLetter Check"], [configPoint="CoverLetter Requirement"], '
                    +'[configPoint="CoverLetter Check"], [configPoint="Latex Check"],'
                    +'[configPoint="Review Decline Notify"], [configPoint="Review Finish Num Notify"], '
                    +'[configPoint="Review OverDue Notify"]').click(
                    function(){
                        if($(this).hasClass('checked')){
                            $(this).removeClass('checked');
                        }else{
                            $(this).addClass('checked');
                        }
                        if( $(this).attr('configPoint') == 'CoverLetter Check'){
                            if($(this).hasClass('checked')){
                                _that.container.find('[configPoint="CoverLetter Requirement"]').removeClass('checked').addClass('checked');
                            }
                        }

                        if( $(this).attr('configPoint') == 'Review Finish Num Notify'){
                            if(!$(this).hasClass('checked')){
                                _that.container.find('[configPoint="Review Result Num"]').val('');
                            }
                        }

                        if( $(this).attr('configPoint') == 'Review OverDue Notify'){
                            if(!$(this).hasClass('checked')){
                                _that.container.find('[configpoint="Review OverDue Notify Time"], [configpoint="Review OverDue Notify Period"]').val('');
                            }
                        }
                    }
                )

                var inputCells = _that.container.find("div[cell]");
                inputCells.each(
                    function(){
                        _that.wd.init($(this), 'input');
                    }
                )
                
                var configPoints = [
                    "Revision Due", "Response Due", "Review Due", "Review Score Module", 
                    "Submit Guidelines", "Submit Requirements", "Require Reviewers Num", "Reviewer Requirement", "CoverLetter Check",
                    "CoverLetter Requirement", "Latex Check", "Review Decline Notify", "Review Finish Num Notify" , "Review OverDue Notify",
                    "Review Result Num", "Review OverDue Notify Time", "Review OverDue Notify Period"
                ]; //JSON.parse( _that.container.find('#configpoints').attr("points"));
                configPoints.forEach(
                    function(c){
                       $.post(
                           '/journal/getSubmitReviewSetting',
                           {configPoint: c},
                           function( rst ){
                                _that.fillConfigPoint(c, rst.configContent);
                           }
                       )
                    }
                )
            }

            this.fillConfigPoint = function( cp, data ){
                //console.log(cp);
                switch( cp ){
                    case "Review Due":
                        _that.container.find('[data-reviewdue]').val( data );
                        break;
                    case "Revision Due":
                        _that.container.find('[data-revisiondue]').val( data );
                        break;
                    case 'Response Due':
                        _that.container.find('[data-responsedue]').val( data );
                        break;
                    case 'Require Reviewers Num':
                        _that.container.find('#ReviewersNum').val( data );
                        break;
                    case 'Submit Requirements': //要不要审稿人
                        if(data == 'true'){
                            _that.container.find('[configPoint="'+ cp +'"]').click();
                        }
                        break;
                    case 'Review OverDue Notify Time':
                    case 'Review OverDue Notify Period':
                    case 'Review Result Num': 
                        _that.container.find('[configPoint="'+ cp +'"]').val(data);
                        break;
                    case 'Latex Check':
                    case 'CoverLetter Check':
                    case 'CoverLetter Requirement':
                    case 'Review Score Module':
                    case 'Review Decline Notify':
                    case 'Review Finish Num Notify':
                    case 'Review OverDue Notify':
                        if(data == 'true'){
                            _that.container.find('[configPoint="'+ cp +'"]').addClass('checked');
                        }
                        break;
                    case 'Submit Guidelines':
                        _that.ed1.set(data);
                        break;
                    case 'Reviewer Requirement':
                        _that.ed2.set(data);
                        break;
                }
            }
        }

        return SubmitReview;
    }
    
)