// Validación del lado del cliente (Bootstrap) + modal de confirmación para eliminar.
(function () {
  'use strict';

  // 1. Validación de formularios antes de enviarlos
  document.querySelectorAll('form.needs-validation').forEach(function (form) {
    form.addEventListener('submit', function (evento) {
      if (!form.checkValidity()) {
        evento.preventDefault();
        evento.stopPropagation();
      }
      form.classList.add('was-validated');
    });
  });

  // 2. El modal toma el nombre y la URL de eliminación del botón que lo abrió
  var modal = document.getElementById('modalEliminar');
  if (modal) {
    modal.addEventListener('show.bs.modal', function (evento) {
      var boton = evento.relatedTarget;
      document.getElementById('nombreEliminar').textContent = boton.getAttribute('data-nombre');
      document.getElementById('formEliminar').setAttribute('action', boton.getAttribute('data-action'));
    });
  }

  // 3. Los mensajes de éxito se cierran solos
  document.querySelectorAll('.auto-dismiss').forEach(function (alerta) {
    setTimeout(function () {
      if (window.bootstrap) { bootstrap.Alert.getOrCreateInstance(alerta).close(); }
    }, 5000);
  });
})();
