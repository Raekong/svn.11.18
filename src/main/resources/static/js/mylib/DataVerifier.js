
define(
	function(){
       var DataVerifier = {};

       DataVerifier.qj2bj = function(str) {
            var tmp = "";
            for (var i = 0; i < str.length; i++) {
                if (str.charCodeAt(i) >= 65281 && str.charCodeAt(i) <= 65374) {// 如果位于全角！到全角～区间内
                    tmp += String.fromCharCode(str.charCodeAt(i) - 65248)
                } else if (str.charCodeAt(i) == 12288) {// 全角空格的值，它没有遵从与ASCII的相对偏移，必须单独处理
                    tmp += ' ';
                } else {// 不处理全角空格，全角！到全角～区间外的字符
                    tmp += str[i];
                }
            }
            return tmp;
        };


        DataVerifier.verifyEmail = function(email) {
            var myreg = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
            var flag = true;
            if (!myreg.test(email.trim())) {
                flag = false;
            }
            return flag;
        };	

        DataVerifier.isInt = function(value){
            return /^(\-)?[0-9]+$/.test(value);
        }
        
        DataVerifier.isDouble = function(value){
            return /^\d+(\.\d+)?$/.test(value);
        }

        DataVerifier.isObject = function( obj ){
            return Object.prototype.toString.call(obj) === '[object Object]';
        }


        DataVerifier.verifyDay = function( value ){
            return /^\d{4}(\-|\/|\.)\d{1,2}\1\d{1,2}$/.test(value);
        }

        DataVerifier.verifyYear = function( value ){
            return /^\d{4}$/.test(value);
        }
        
        DataVerifier.verify = function( value, type){
            var flag = false;
            if( !value ) return false;
            switch( type ){
                case 'email': 
                    flag = this.verifyEmail ( value );
                    break;
                case 'nonull':
                    flag = !!value;
                    break;
                case 'int':
                    flag = this.isInt(value);
                    break;
                case 'double':
                    flag = this.isDouble(value);
                    break;
                case 'date':
                    flag = this.verifyDay(value);
                    break;
                case 'year':
                    flag = this.verifyYear(value);
                    break;
            }
            return flag;
        }

        DataVerifier.verifyFailMsg = {
            'email' : 'The format of the input EMAIL address is incorrect! ',
            'nonull' : 'The input is required!',
            'int' : 'Please input a Number!',
            'date' : 'The input date format is incorrect!'
        }
    
       return DataVerifier;
})