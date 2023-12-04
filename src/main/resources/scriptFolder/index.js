    let signaturePad
    let signatureData;

    document.getElementById("reset-button").addEventListener("click", function(){
        signaturePad.clear();

    });
    document.getElementById("submit-button").addEventListener("click", function(){
        signatureData = signaturePad.toDataURL();
        console.log(signatureData);
        var pdf = new jsPDF();
        pdf.addImage(signatureData, 'JPEG', 10, 10);
        // pdf.save("download.pdf");
        
    },false);

    document.addEventListener("DOMContentLoaded", function(){
        const canvas = document.getElementById("signature-pad");
        signaturePad = new SignaturePad(canvas);
    });