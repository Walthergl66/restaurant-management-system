from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from PIL import Image, ImageDraw, ImageFont
import os

ROOT = r'C:\Users\USER PC\Documents\restaurant-management-system'
OUT = os.path.join(ROOT,'docs','entregas','Practica_Individual_Tema_2_3_IS803.docx')
PNG = os.path.join(ROOT,'docs','entregas','diagrama.png')

im=Image.new('RGB',(1500,340),'white'); d=ImageDraw.Draw(im)
regular=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',26); bold=ImageFont.truetype('C:/Windows/Fonts/arialbd.ttf',28)
boxes=[(35,90,410,265,'Origen A · SPI móvil\nprecio · extraId'),(560,90,945,265,'Adaptador\nmapear · normalizar · validar'),(1090,90,1465,265,'Contrato común\nPedidoNormalizado · UI móvil')]
for x1,y1,x2,y2,label in boxes:
 d.rounded_rectangle((x1,y1,x2,y2),20,fill='#FFF3E8' if x1==560 else '#F1F5F9',outline='#C2410C' if x1==560 else '#334155',width=4)
 for n,line in enumerate(label.split('\n')):
  f=bold if n==0 else regular; b=d.textbbox((0,0),line,font=f); d.text((x1+(x2-x1-(b[2]-b[0]))/2,y1+42+n*48),line,font=f,fill='#172033')
d.rounded_rectangle((35,15,410,75),14,fill='#E0F2FE',outline='#0369A1',width=3); d.text((76,30),'Origen B · REST web',font=bold,fill='#172033')
for y in (135,220):
 d.line((410,y,545,y),fill='#C2410C',width=6); d.polygon([(545,y),(525,y-12),(525,y+12)],fill='#C2410C')
d.line((955,177,1075,177),fill='#C2410C',width=6);d.polygon([(1075,177),(1055,165),(1055,189)],fill='#C2410C');im.save(PNG)

doc=Document(); sec=doc.sections[0];sec.top_margin=Inches(.65);sec.bottom_margin=Inches(.65);sec.left_margin=Inches(.75);sec.right_margin=Inches(.75)
doc.styles['Normal'].font.name='Aptos';doc.styles['Normal'].font.size=Pt(9.5);doc.styles['Normal'].font.color.rgb=RGBColor(35,45,60);doc.styles['Normal'].paragraph_format.space_after=Pt(5)
for name,size,color in [('Title',24,'172033'),('Heading 1',16,'9A3412'),('Heading 2',12,'C2410C')]:
 s=doc.styles[name];s.font.name='Aptos Display';s.font.size=Pt(size);s.font.bold=True;s.font.color.rgb=RGBColor.from_string(color)
 s.paragraph_format.space_before=Pt(8);s.paragraph_format.space_after=Pt(4)
 ppr= s.element.get_or_add_pPr()
 for bdr in ppr.findall(qn('w:pBdr')): ppr.remove(bdr)
def table(headers,rows):
 t=doc.add_table(rows=1,cols=len(headers));t.style='Light Shading Accent 1';t.alignment=WD_TABLE_ALIGNMENT.CENTER
 for i,x in enumerate(headers):
  c=t.rows[0].cells[i];c.text=x;tc=c._tc.get_or_add_tcPr();sh=OxmlElement('w:shd');sh.set(qn('w:fill'),'9A3412');tc.append(sh)
  for r in c.paragraphs[0].runs:r.font.bold=True;r.font.color.rgb=RGBColor(255,255,255);r.font.size=Pt(8)
 for row in rows:
  cells=t.add_row().cells
  for i,x in enumerate(row):
   cells[i].text=x;cells[i].vertical_alignment=WD_CELL_VERTICAL_ALIGNMENT.CENTER
   for p in cells[i].paragraphs:
    p.paragraph_format.space_after=Pt(2)
    for r in p.runs:r.font.size=Pt(8)
 doc.add_paragraph().paragraph_format.space_after=Pt(1)
def bullet(x):doc.add_paragraph(x,style='List Bullet')
head=sec.header.paragraphs[0];head.text='IS-803  |  PRÁCTICA INDIVIDUAL  |  TEMA 2.3';head.alignment=WD_ALIGN_PARAGRAPH.RIGHT
for r in head.runs:r.font.size=Pt(8);r.font.color.rgb=RGBColor(120,130,145)
foot=sec.footer.paragraphs[0];foot.alignment=WD_ALIGN_PARAGRAPH.CENTER;r=foot.add_run('Implementación de patrones de traducción y normalización');r.font.size=Pt(8);r.font.color.rgb=RGBColor(120,130,145)
p=doc.add_paragraph(style='Title');p.add_run('Implementación de patrones de traducción y normalización')
p=doc.add_paragraph();r=p.add_run('Práctica individual · Tema 2.3 · IS-803');r.bold=True;r.font.size=Pt(13);r.font.color.rgb=RGBColor(154,52,18)
table(['Estudiante','Curso / paralelo','Fecha'],[['____________________________','8.º nivel / A','____________']])
doc.add_paragraph('Completa tu nombre, código y fecha; adjunta tu propia evidencia de ejecución antes de entregar.')
doc.add_heading('Propósito y resultado',1)
doc.add_paragraph('En Estación Burger, los canales móvil y web reciben pedidos con estructuras JSON diferentes. Implementé un adaptador en la feature móvil que convierte ambas entradas al modelo PedidoNormalizado, valida campos, importes y fechas, y conserva intactos los contratos de la API. Tres pruebas automatizadas comprueban equivalencia y errores controlados.')
doc.add_heading('Escenario y conflicto estructural',1)
doc.add_paragraph('El backend ofrece dos representaciones del mismo pedido. PedidoClienteSPI usa precio y extraId, incluye subtotal global y versión. PedidoClienteResponse usa precioUnitario e id; no incluye subtotal global ni versión. Ambos transportan montos y fechas ISO. Sin traducción centralizada, cada pantalla tendría que conocer las diferencias y podría mostrar el mismo pedido de forma distinta.')
table(['Aspecto','Origen A · SPI móvil','Origen B · REST web'],[
['Precio unitario','lineas[].precio','lineas[].precioUnitario'],['ID del extra','extras[].extraId','extras[].id'],['Subtotal global','subtotal explícito','se deriva de líneas'],['Versión interna','version','ausente'],['Fechas','creadoAt / actualizadoAt','creadoAt / actualizadoAt'],['Montos','número JSON','BigDecimal serializado; texto decimal tolerado']])
doc.add_heading('Contrato común y tabla de mapeo',1)
doc.add_paragraph('PedidoNormalizado es un modelo de presentación común. Los importes son numéricos redondeados a centavos y las fechas se representan como ISO-8601 en UTC.')
table(['Campo común','SPI móvil','REST web','Regla'],[
['codigo / estado','codigo / estado','codigo / estado','Requeridos; recortar texto'],['pago / entrega','metodoPago / metodoEntrega','mismos campos','Recortar y convertir a mayúscula'],['subtotal','subtotal','suma de subtotales de línea','Importe no negativo'],['costoEntrega / total','costoEntrega / total','costoEntrega / total','Validar total = subtotal + envío'],['creadoEn / actualizadoEn','creadoAt / actualizadoAt','mismos campos','Validar zona y convertir a UTC'],['precioUnitario','precio','precioUnitario','Unificar nombre y tipo decimal'],['extras[].id','extraId','id','Entero positivo'],['versión técnica','version','ausente','Excluir del modelo visual']])
doc.add_heading('Reglas e implementación',1)
doc.paragraphs[-1].paragraph_format.keep_with_next=True
doc.add_paragraph('El archivo mobile/src/features/pedidos/normalizarPedido.ts contiene desdeSpi y desdeRespuestaWeb; ambas delegan a un conversor compartido y producen el mismo modelo. El adaptador es puro, se ubica en la frontera de la feature y no modifica el JSON recibido ni hace llamadas de red.')
doc.paragraphs[-1].paragraph_format.keep_with_next=True
for x in ['Texto obligatorio no vacío; montos finitos y no negativos.','Cantidades e identificadores enteros positivos; estructuras anidadas con forma válida.','Fechas ISO con zona horaria, convertidas a UTC.','Total coherente con subtotal y costo de entrega, con tolerancia de un centavo.','Errores explícitos ante datos ambiguos o incorrectos.']:bullet(x)
doc.add_heading('Diagrama del proceso',1)
p=doc.add_paragraph();p.alignment=WD_ALIGN_PARAGRAPH.CENTER;p.add_run().add_picture(PNG,width=Inches(6.7))
p=doc.add_paragraph('Figura 1. Los dos contratos del backend convergen en un modelo común de presentación.');p.alignment=WD_ALIGN_PARAGRAPH.CENTER
for r in p.runs:r.italic=True;r.font.size=Pt(8)
doc.add_heading('Pruebas y resultados',1)
doc.add_paragraph('Ejecuté las pruebas automatizadas con Node.js y el test runner incorporado. Se usan datos ficticios. P1 y P2 comprueban la conversión y la interoperabilidad; P3 verifica el rechazo explícito de errores.')
table(['Caso','Entrada','Resultado observado'],[
['P1 · SPI válido','precio=10; extraId=4; método con espacios; fecha -05:00','Pasa: texto limpio, mayúscula y fecha UTC.'],['P2 · REST equivalente','precioUnitario="10.00"; extras.id=4; costo="1.50"','Pasa: salida profunda igual a P1.'],['P3 · inválidos','total=99 para subtotal 20 + envío 1.50; fecha sin zona','Pasa: datos rechazados con error explícito.']])
code=doc.add_paragraph();code.paragraph_format.left_indent=Inches(.15);rr=code.add_run('node --experimental-strip-types --test tests/normalizarPedido.test.mjs\n# tests 3 · pass 3 · fail 0');rr.font.name='Consolas';rr.font.size=Pt(8)
doc.add_heading('Conclusión y reflexión individual',1)
doc.add_paragraph('Resolví la incompatibilidad entre dos estructuras de pedidos reales del proyecto: cambian los nombres de precio e identificador de extra, el DTO web omite algunos campos, y los importes y fechas requieren normalización. Centralicé las reglas en un adaptador puro para que los componentes reciban un contrato estable. Verifiqué que formatos equivalentes produzcan la misma salida y que entradas inválidas generen errores controlados.')
doc.add_heading('Fuentes y archivos del proyecto',1)
bullet('Implementación: mobile/src/features/pedidos/normalizarPedido.ts')
bullet('Pruebas: mobile/tests/normalizarPedido.test.mjs')
bullet('Contratos: backend/src/main/java/com/restaurante/clientes/PedidoClienteSPI.java y backend/src/main/java/com/restaurante/clientes/web/dto/PedidoClienteResponse.java')
bullet('Documentación del adaptador: docs/api/patrones-traduccion-pedidos.md')
doc.core_properties.title='Implementación de patrones de traducción y normalización';doc.core_properties.subject='Práctica individual IS-803 Tema 2.3';doc.save(OUT);print(OUT)
