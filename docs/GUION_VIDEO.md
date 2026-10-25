# Guion del video (3–5 minutos, nivel A2–B1)

> **Antes de grabar:** ejecuta la aplicación y comprueba cada paso de la demostración. Habla solo de lo que hayas visto funcionar en tu computador. Ritmo sugerido: ~130 palabras por minuto (el guion tiene ~480 palabras ≈ 3,7 minutos).
> Las indicaciones en español entre corchetes son para ti: no se leen en voz alta.

---

## Guion en inglés

**1. Introduction** (~30 s)
[Mostrar la página principal de la aplicación en el navegador.]

Hello! My name is Wilmer, and I am a software engineering student. In this video, I will show you my project: the Product Management System.

**2. Project objective** (~30 s)
[Mostrar el título y las tres tarjetas del panel.]

The goal of this project is to manage the products of a small store. With this application, the user can create, read, update, and delete products. This is called CRUD. The most important point is that the data is saved in a database, so it does not disappear when we close the program.

**3. Technologies used** (~30 s)
[Mostrar la estructura de carpetas en el editor de código.]

I used Java 21 and Spring Boot for the backend. For the web pages, I used Thymeleaf, HTML, CSS, and Bootstrap. The database is SQLite. I connect Java with SQLite using JDBC. I also used Maven to build the project and JUnit to test it.

**4. Database structure** (~30 s)
[Mostrar el archivo schema.sql.]

This is the database structure. There is one table called "productos". It has five columns: code, name, description, price, and quantity. The code is the primary key, so two products cannot have the same code. The price and the quantity cannot be negative, and the database checks this rule too.

**5. CRUD demonstration** (~60 s)
[Volver al navegador y hacer cada acción despacio.]

Now, let me show you the application. First, I create a product. I write the code, the name, the description, the price, and the quantity. I click "Save", and the new product appears in the table. The cards at the top update the total products and the inventory value.

Next, I try the same code again. The system shows a clear error message, and the application keeps working. Now I search for a product by name. [Escribir un nombre en el buscador.] The table shows only the matching products.

To update a product, I click "Edit", I change the price, and I save. Finally, to delete a product, I click "Delete". A window asks me to confirm. I confirm, and the product disappears.

**6. Data persistence demonstration** (~40 s)
[Mostrar la terminal, detener con Ctrl+C, mostrar el archivo data/productos.db y volver a iniciar.]

Now I will show the persistence. I stop the application. Here is the file "productos.db" in the data folder. I start the application again. [Abrir el navegador.] The products are still here. This is because SQLite stores the data in a real file on my computer.

**7. JDBC and PreparedStatement** (~40 s)
[Mostrar el código de ProductoDAO.java, resaltando el signo ? y el try-with-resources.]

This is the DAO class. JDBC is the Java tool that connects my program with the database. Here I use a PreparedStatement. You can see the question marks in the SQL. The values go in separately, with methods like setString. This is important for security, because it protects the application from SQL injection. I also use try-with-resources to close the connection automatically.

**8. Conclusion** (~20 s)
[Mostrar de nuevo el panel principal.]

In conclusion, this project has a complete CRUD, a real SQLite database, and a web interface. I learned how JDBC works and why layers are important. Thank you for watching!

*(Opcional, solo si publicas en Render:)* "I also published a free demo online. In the free plan, the data can be lost when the server restarts, so the permanent data is in the local version."

---

## Traducción al español (para estudiar)

**1. Introducción**
¡Hola! Me llamo Wilmer y soy estudiante de ingeniería de software. En este video les mostraré mi proyecto: el Sistema de Gestión de Productos.

**2. Objetivo del proyecto**
El objetivo de este proyecto es administrar los productos de una tienda pequeña. Con esta aplicación, el usuario puede crear, leer, actualizar y eliminar productos. Esto se llama CRUD. El punto más importante es que los datos se guardan en una base de datos, así que no desaparecen cuando cerramos el programa.

**3. Tecnologías utilizadas**
Usé Java 21 y Spring Boot para el backend. Para las páginas web usé Thymeleaf, HTML, CSS y Bootstrap. La base de datos es SQLite. Conecto Java con SQLite mediante JDBC. También usé Maven para construir el proyecto y JUnit para probarlo.

**4. Estructura de la base de datos**
Esta es la estructura de la base de datos. Hay una tabla llamada «productos». Tiene cinco columnas: código, nombre, descripción, precio y cantidad. El código es la clave primaria, así que dos productos no pueden tener el mismo código. El precio y la cantidad no pueden ser negativos, y la base de datos también verifica esta regla.

**5. Demostración del CRUD**
Ahora les muestro la aplicación. Primero creo un producto. Escribo el código, el nombre, la descripción, el precio y la cantidad. Hago clic en «Guardar» y el nuevo producto aparece en la tabla. Las tarjetas de arriba actualizan el total de productos y el valor del inventario.

Luego intento usar el mismo código otra vez. El sistema muestra un mensaje de error claro y la aplicación sigue funcionando. Ahora busco un producto por nombre. La tabla muestra solo los productos que coinciden.

Para actualizar un producto, hago clic en «Editar», cambio el precio y guardo. Finalmente, para eliminar un producto, hago clic en «Eliminar». Una ventana me pide confirmar. Confirmo y el producto desaparece.

**6. Demostración de la persistencia**
Ahora mostraré la persistencia. Detengo la aplicación. Aquí está el archivo «productos.db» en la carpeta data. Inicio la aplicación otra vez. Los productos siguen aquí. Esto es porque SQLite guarda los datos en un archivo real en mi computador.

**7. JDBC y PreparedStatement**
Esta es la clase DAO. JDBC es la herramienta de Java que conecta mi programa con la base de datos. Aquí uso un PreparedStatement. Se ven los signos de interrogación en el SQL. Los valores se envían por separado, con métodos como setString. Esto es importante para la seguridad, porque protege la aplicación de la inyección SQL. También uso try-with-resources para cerrar la conexión automáticamente.

**8. Conclusión**
En conclusión, este proyecto tiene un CRUD completo, una base de datos SQLite real y una interfaz web. Aprendí cómo funciona JDBC y por qué son importantes las capas. ¡Gracias por ver el video!

*(Opcional)* «También publiqué una demostración gratuita en línea. En el plan gratuito, los datos pueden perderse cuando el servidor se reinicia, así que los datos permanentes están en la versión local».

---

## Vocabulario útil
| English | Español |
|---|---|
| to create / read / update / delete | crear / leer / actualizar / eliminar |
| database | base de datos |
| primary key | clave primaria |
| table, column | tabla, columna |
| to save | guardar |
| to search | buscar |
| security | seguridad |
| to close | cerrar |
| layer | capa |
