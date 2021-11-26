define(
    ["widget", "datacenter", "editor"],
    function( wd, dc, ed ){
        function Payment(container){
            this.container = container;
            var _that = this;
            this.lang = $('#frame').attr('i18n');
            this.wd = new wd();

            this.init = function(){
                
                this.initPayment();
                var isZh = $('#frame').attr('i18n') == 'true';
                this.ed1 = new ed('editor-wiretransfer', isZh, 288);
                _that.ed1.init();

                this.ed2 = new ed('editor-bankinfo', isZh, 208);
                _that.ed2.init();

                this.container.find('[submit]').click(
                    function(){
                        var point = $(this).attr('submit');
                        switch( point ){
                            case 'payment':
                                _that.submitPayment();
                        }
                    }
                );
                this.loadWorkFlowDefine();
            }

            this.loadWorkFlowDefine = function(){
                var configPoints = [
                    'Payment', 'Article Processing Charge', 'Over Charge', 'Online Transfer Fee',
                    'Wire Transfer', 'Basic pages Number', 'About Article Processing Charge',
                    'Bank Account Information'
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
                            if( _that.container.find('[configPoint="Payment"]').hasClass('checked')){
                                _that.container.find('#paymentboard').show();
                            }
                        },200
                    )
                    
                }
            }

            this.fillConfigPoint = function( cp, data ){
                
                switch( cp ){
                    case 'Payment': 
                        if(data == 'true'){
                            _that.container.find('[configPoint="'+ cp +'"]').addClass('checked');
                        }
                        break;
                    case 'Article Processing Charge':
                    case 'Over Charge':
                    case 'Online Transfer Fee':
                    case 'Wire Transfer':
                    case 'Basic pages Number':
                        _that.container.find('[configPoint="'+ cp +'"] input').val(data);
                        break;
                    case 'About Article Processing Charge':
                        _that.ed1.set(data);
                        break;
                    case 'Bank Account Information':
                        _that.ed2.set(data);
                        break;
                }
                _that.loadFinal();
            }

            //==================初始化支付-------------------------------------------
            this.paymentdc = {
                'data-basiccharge': {
                    'verify': 'double',
                    'val' : ''
                },
                'data-superpagefee': {
                    'verify': 'double',
                    'val' : ''
                },
                'data-onlinetransferfee': {
                    'verify': 'double',
                    'val' : ''
                },
                'data-handlingfee': {
                    'verify': 'double',
                    'val' : ''
                },
                'data-basicpages': {
                    'verify': 'int',
                    'val' : ''
                }
            }
            this.initPayment = function(){
                var inputCells = this.container.find("#payment div[cell]");
                inputCells.each(
                    function(){
                        _that.wd.init($(this), 'input');
                    }
                );
                this.container.find('[configpoint="Payment"]').click(
                    function(){
                        var span = $(this);
                        if($(this).hasClass('checked')){
                            $(this).removeClass("checked");
                            _that.container.find('#paymentboard input').val('');
                            _that.container.find('#paymentboard').hide();
                        }else{
                            $.post(
                                '/publisher/canPayment',
                                {pid: $('#frame').attr('pid')},
                                function(rst){
                                    if( rst == "true"){
                                        span.addClass('checked');
                                        _that.container.find('#paymentboard').show();
                                    }else{
                                        _that.wd.msgbox(
                                            _that.lang == 'true',
                                            'alert',
                                            'Message',
                                            'The payment module of the publisher has not configured yet.'
                                        )
                                    }
                                }
                            )
                        }
                    }
                )
            }

            this.submitPayment = function(){
                //如果不需要支付
                if(!_that.container.find('[configPoint="Payment"]').hasClass('checked')){
                    var tmp = {};
                    tmp.configPoint = 'Payment';
                    tmp.configContent = 'false';
                    $.post(
                        '/journal/save',
                        tmp,
                        function(rst){
                            _that.wd.msgbox(
                                _that.lang == 'true',
                                'info',
                                'Message',
                                '配置项设置成功'
                            )
                        }
                    )
                    return;
                }

                _that.container.find('#paymentboard').find('.error').removeClass('error');
                var paydc = new dc( _that.container.find('#paymentboard'), _that.paymentdc );
                var data = paydc.exec();
                if( !!data.verify.key ){
                    data.verify.dom.addClass('error').attr('placeholder', 'Please enter an integer or a number');
                    return;
                }
                
                _that.container.find('#payment').find('[configPoint]').each(function(){
                    var tmp = {};
                    tmp.configPoint = $(this).attr('configPoint');
                    if(tmp.configPoint == 'Payment'){
                        tmp.configContent = 'true';
                    }else if(tmp.configPoint == 'About Article Processing Charge'){
                        tmp.configContent = _that.ed1.html();
                    }else if(tmp.configPoint == 'Bank Account Information'){
                        tmp.configContent = _that.ed2.html();
                    }else{
                        tmp.configContent = $(this).find('input').val();
                    }

                    $.post(
                        '/journal/save',
                        tmp,
                        function(rst){
                            
                        }
                    )
                });

                _that.wd.msgbox(
                    _that.lang == 'true',
                    'info',
                    'Message',
                    'The configuration has set successfully.'
                )
            }

        }

        return Payment;
    }
)