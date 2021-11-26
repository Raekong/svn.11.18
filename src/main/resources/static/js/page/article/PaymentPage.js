define(
    ["widget",  "eventcenter",  "uploader", "filedownloader", "popup", "mystripe", "confirm","dataverifier" ],
    function(wd, ec, uploader,downloader, popup, mystripe, confirm, dv){
        function Payment( container ){
            this.name = 'PaymentTab';
            this.container = container;
            this.wd = new wd();
            this.ec = new ec('debug');
            this.ec.addHandler( this ); 

            var _that = this;

            this.init = function(){
                var inputCells = _that.container.find("#origin div[cell]");
                inputCells.each(
                    function(){
                        _that.wd.init($(this), 'input');
                        var data = $(this).attr('data');
                        $(this).find('input').val($(this).attr(data));
                        var isPaied = ($('[total]').attr('total') != 0)
                        if(data != 'totalpage' || isPaied){
                            $(this).find('input').attr('readonly', 'readonly').css('background-color', '#f2f2f2');
                        }else{
                            $(this).find('input').keyup(
                                _that.pageReset
                            )
                        }
                    }
                )

                this.container.find('#wiretransfer').click(
                    _that.uploadRecipt
                )

                this.container.find('#online').click(
                    _that.online
                )

                this.container.find('#filelist .row.down').click(
                    _that.downloadArticleFile
                )

                this.container.find('.row .down').click(
                    _that.downReceipt
                )

                this.container.find('#auditPass').click(
                    _that.auditPass
                )
            }

            this.auditPass = function(){
                _that.confirm = new confirm(
                    'Payment Audit', 
                    'Are you sure that the payment has successfully completed ?', 
                    _that.lang=='true'? 'zh': 'en',  
                    function(dom){
                        dom.find('#cancel').click(function(){
                            _that.confirm.close();
                        })

                        dom.find('#save').click(function(){
                            $.post(
                                '/payment/done',
                                { aid:aid, jid:$('#frame').attr('jid') },
                                function(){
                                    window.location.reload();
                                }
                            )
                        })
                    }, '488px');
                _that.confirm.confirm();
            }

            this.downloadArticleFile = function(){
                new downloader().download($(this).attr('link'), $(this).attr('origin'));
            }

            this.downReceipt = function(){
                var type = $(this).attr('type');
                var fileId = $(this).attr('fileId');

                $.post(
                    '/payment/downreceipt',
                    {type:type, fid: fileId},
                    function(rst){
                        console.log(rst);
                        if( type == "Wire Transfer"){
                            new downloader().download(rst.link, rst.fileName);
                        }else{
                            window.open(rst.link);
                        }
                    }
                )
            }

            this.online = function(){
                var data = {};
                data.payid = $('#paymentPage').attr('originpayid');
                data.paynum = _that.container.find('[data-totalpage]').val();
                data.paytotal = _that.container.find('[data-online]').val();
                data.type = 'online';
                data.isBack = false;
                data.aid = $('#paymentPage').attr('aid');
                console.log(data);
                $.post(
                    '/payment/stripe',
                    data,
                    function( rst ){
                        var dom = $(rst);
                        var title = 'Stripe Payment';
                        _that.pop = new popup(title, dom,  '788px', function(dom, index){
                            var stripe = new mystripe(dom);
                            stripe.init();
                            dom.find('#cancel').click(
                                function(){
                                    _that.pop.close();
                                }
                            )
                        });
                        _that.pop.pop();
                    }
                )
            }

            this.uploadRecipt = function(){
                $.post(
                    '/submit/submit-upload-pop/' + (this.lang ? 'zh' : 'en'),
                    function(rst){
                        var dom = $(rst);
                        var title = 'Upload Bank Recipet Slip';
                        
                        _that.pop = new popup(title, dom,  '788px', function(dom, index){
                            dom.find('.title span').text(title);
                            dom.find('#fileTypeLabel').text('Recipet File');
                            //新增一个输入框,用以输入用户的邮箱
                            dom.find('#fileTypeLabel').parent().prepend("<div><label>Payer's Email<span style='color:red'>*</span></label><input id='payeremail'></input></div>");
                            //自动填充用户的邮件
                            $.post(
                                '/payment/useremail',
                                function(rst){
                                    if( !!rst ){
                                        dom.find('#payeremail').val(rst);
                                    }
                                }
                            )

                            //去掉其它可选项
                            dom.find('.autolist li:not(:contains('+ fileType +'))').remove();
                            dom.find('#fileType').val('Bank Recipet Slip').attr('readonly', 'readonly');
                            //去掉文件列表
                            dom.find('#uploadfiles').remove();

                            var uploader = _that.initUploader(dom.find('#uploadDiv'), _that.ec, null, (this.lang== 'true'));
                            uploader.init();
                            

                            dom.find('#cancel').click(
                                function(){
                                    _that.pop.close();
                                }
                            )
                        });
                        _that.pop.pop();
                    }
                )
            }

            this.initUploader = function(dom, ec, uploadBut, lang){
                var up = new uploader(
                    dom, 
                    ec,
                    $('#uploadConfirm'),  //确定上传按键
                    lang
                );
                return up;
            }


            this.pageReset = function(){
                var totalPage = $(this).val();
                if(! /^(\-)?[0-9]+$/.test(totalPage) ){
                    $(this).addClass('error');
                    return;
                }else{
                    $(this).removeClass('error');
                    $.post(
                        '/payment/countAndApc',
                        {aid: $('#paymentPage').attr('aid'), jid: $('#paymentPage').attr('jid'), 'pnum': totalPage},
                        function(rst){
                            _that.container.find('#origin [data=totalapc] input').val(rst.totalAPC);
                            _that.container.find('#origin [data=wiretrnasfer] input').val(rst.totalWire);
                            _that.container.find('#origin [data=online] input').val(rst.totalOnline);
                            
                        }
                    )
                }
                
            }

            this.wiredata = {};
            this.payedWrie = function(file){
                var data = {};
                data.payid = $('#paymentPage').attr('originpayid');
                data.paynum = _that.container.find('[data-totalpage]').val();
                data.paytotal = _that.container.find('[data-wiretrnasfer]').val();
                var email = $('#payeremail').val();
                if(!email && !dv.verifyEmail(email)){
                    _that.wiredata = file;
                    $('#payeremail').addClass('error');
                    return;
                }
                data.email = email;
                data.type = 'wire';
                data.isBack = false;
                data.aid = $('#paymentPage').attr('aid');
                //防止用户因为没有填写邮箱,被阻止后,重新提交,但没有上传材料文件
                if(!!file.originName){
                    _that.wiredata = file;
                }
                data = $.extend(data, _that.wiredata);
                console.log(data);

                $.post(
                    '/payment/wire',
                    data,
                    function( rst ){
                        window.location.reload();
                    }
                )
            } 

            this.exports = {
                'uploaded' : this.payedWrie
            }
        }

        return Payment;
    }
)