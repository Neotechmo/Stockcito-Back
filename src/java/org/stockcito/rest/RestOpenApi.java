package org.stockcito.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("v3/api-docs")
public class RestOpenApi {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response docs() {
        String json = """
            {
              "openapi": "3.0.3",
              "info": {
                "title": "Stockcito API",
                "version": "1.0.0",
                "description": "API REST de inventario. Usa Authorize para ingresar el token obtenido en /v1/auth/login."
              },
              "servers": [{"url": "/Stockcito/api"}],
              "tags": [
                {"name": "Auth"}, {"name": "Users"}, {"name": "Categories"},
                {"name": "Suppliers"}, {"name": "Products"}, {"name": "Inventory"},
                {"name": "Purchase Requests"}, {"name": "Imports"}, {"name": "AI"}
              ],
              "security": [{"bearerAuth": []}],
              "components": {
                "securitySchemes": {
                  "bearerAuth": {"type": "http", "scheme": "bearer", "bearerFormat": "JWT"}
                },
                "parameters": {
                  "Id": {
                    "name": "id", "in": "path", "required": true,
                    "schema": {"type": "integer", "format": "int64", "minimum": 1}
                  }
                },
                "responses": {
                  "Ok": {"description": "Operación exitosa"},
                  "Created": {"description": "Recurso creado"},
                  "BadRequest": {"description": "Petición inválida"},
                  "Unauthorized": {"description": "Token JWT ausente o inválido"},
                  "NotFound": {"description": "Recurso no encontrado"},
                  "ServiceUnavailable": {"description": "Servicio externo no disponible o no configurado"}
                },
                "schemas": {
                  "LoginRequest": {
                    "type": "object", "required": ["email", "password"],
                    "properties": {
                      "email": {"type": "string", "format": "email", "example": "admin@stockcito.com"},
                      "password": {"type": "string", "format": "password", "example": "admin123"}
                    }
                  },
                  "RegisterRequest": {
                    "type": "object", "required": ["name", "email", "password"],
                    "properties": {
                      "name": {"type": "string", "example": "Nuevo Operador"},
                      "email": {"type": "string", "format": "email", "example": "nuevo@stockcito.com"},
                      "password": {"type": "string", "format": "password", "minLength": 6, "example": "password123"}
                    }
                  },
                  "UserRequest": {
                    "type": "object", "required": ["name", "email"],
                    "properties": {
                      "name": {"type": "string", "example": "Usuario Demo"},
                      "email": {"type": "string", "format": "email", "example": "usuario@stockcito.com"},
                      "password": {"type": "string", "format": "password", "minLength": 6, "example": "password123"},
                      "role": {"type": "string", "enum": ["ADMIN", "OPERADOR"], "example": "OPERADOR"}
                    }
                  },
                  "CategoryRequest": {
                    "type": "object", "required": ["name"],
                    "properties": {
                      "name": {"type": "string", "example": "Congelados"},
                      "description": {"type": "string", "example": "Productos almacenados en congelación"}
                    }
                  },
                  "SupplierRequest": {
                    "type": "object", "required": ["name"],
                    "properties": {
                      "name": {"type": "string", "example": "Proveedor Demo"},
                      "contactName": {"type": "string", "example": "Ana Pérez"},
                      "email": {"type": "string", "format": "email", "example": "ventas@proveedor.com"},
                      "phone": {"type": "string", "example": "+52 477 100 0000"},
                      "address": {"type": "string", "example": "León, Guanajuato"},
                      "notes": {"type": "string", "example": "Entrega los lunes"}
                    }
                  },
                  "ProductRequest": {
                    "type": "object", "required": ["categoryId", "name", "unitOfMeasure", "minStock", "currentStock"],
                    "properties": {
                      "categoryId": {"type": "integer", "format": "int64", "example": 1},
                      "supplierId": {"type": "integer", "format": "int64", "nullable": true, "example": 1},
                      "name": {"type": "string", "example": "Arroz Blanco 1 kg"},
                      "sku": {"type": "string", "example": "ARR-002"},
                      "description": {"type": "string", "example": "Arroz de grano largo"},
                      "unitOfMeasure": {"type": "string", "example": "kg"},
                      "minStock": {"type": "number", "format": "double", "minimum": 0, "example": 10},
                      "currentStock": {"type": "number", "format": "double", "minimum": 0, "example": 25},
                      "unitPrice": {"type": "number", "format": "double", "minimum": 0, "example": 18.5},
                      "barcode": {"type": "string", "example": "750000000001"}
                    }
                  },
                  "InventoryMovementRequest": {
                    "type": "object", "required": ["productId", "userId", "movementType", "quantity"],
                    "properties": {
                      "productId": {"type": "integer", "format": "int64", "example": 1},
                      "userId": {"type": "integer", "format": "int64", "example": 1},
                      "movementType": {"type": "string", "enum": ["ENTRY", "EXIT"], "example": "ENTRY"},
                      "quantity": {"type": "number", "format": "double", "exclusiveMinimum": true, "minimum": 0, "example": 10},
                      "movementDate": {"type": "string", "format": "date-time", "nullable": true},
                      "notes": {"type": "string", "example": "Entrada desde proveedor"},
                      "originalText": {"type": "string", "nullable": true}
                    }
                  },
                  "PurchaseRequestBody": {
                    "type": "object", "required": ["productId", "requestedBy", "quantity"],
                    "properties": {
                      "productId": {"type": "integer", "format": "int64", "example": 1},
                      "requestedBy": {"type": "integer", "format": "int64", "example": 2},
                      "approvedBy": {"type": "integer", "format": "int64", "nullable": true},
                      "quantity": {"type": "number", "format": "double", "exclusiveMinimum": true, "minimum": 0, "example": 20},
                      "status": {"type": "string", "enum": ["PENDING", "PURCHASED", "CANCELLED"], "example": "PENDING"},
                      "notes": {"type": "string", "example": "Reposición semanal"}
                    }
                  },
                  "PurchaseStatusRequest": {
                    "type": "object", "required": ["status"],
                    "properties": {
                      "status": {"type": "string", "enum": ["PENDING", "PURCHASED", "CANCELLED"], "example": "PURCHASED"},
                      "approvedBy": {"type": "integer", "format": "int64", "nullable": true, "example": 1}
                    }
                  },
                  "ImportItemRequest": {
                    "type": "object", "required": ["productName", "quantity"],
                    "properties": {
                      "productId": {"type": "integer", "format": "int64", "nullable": true, "example": 1},
                      "productName": {"type": "string", "example": "Arroz Blanco 1 kg"},
                      "movementType": {"type": "string", "enum": ["ENTRY", "EXIT"], "example": "ENTRY"},
                      "quantity": {"type": "number", "format": "double", "exclusiveMinimum": true, "minimum": 0, "example": 5},
                      "unitOfMeasure": {"type": "string", "example": "kg"},
                      "confidence": {"type": "number", "format": "double", "minimum": 0, "maximum": 1, "example": 1},
                      "rawLine": {"type": "string", "example": "Arroz Blanco 1 kg: 5"}
                    }
                  },
                  "ImportPreviewRequest": {
                    "type": "object", "required": ["userId", "items"],
                    "properties": {
                      "userId": {"type": "integer", "format": "int64", "example": 1},
                      "sourceType": {"type": "string", "enum": ["PDF", "IMAGE", "CSV", "MANUAL"], "example": "MANUAL"},
                      "sourceFilename": {"type": "string", "example": "entrada-manual"},
                      "fileHash": {"type": "string", "nullable": true},
                      "rawText": {"type": "string", "nullable": true},
                      "aiModel": {"type": "string", "nullable": true},
                      "notes": {"type": "string", "example": "Importación de prueba"},
                      "items": {"type": "array", "items": {"$ref": "#/components/schemas/ImportItemRequest"}}
                    }
                  },
                  "SummaryRequest": {
                    "type": "object",
                    "properties": {
                      "daysBack": {"type": "integer", "minimum": 1, "maximum": 365, "example": 30, "description": "Dias recientes para analizar salidas y entradas"},
                      "lowStockLimit": {"type": "integer", "minimum": 1, "maximum": 25, "example": 8, "description": "Cantidad maxima de productos con stock bajo a enviar a la IA"},
                      "highExitLimit": {"type": "integer", "minimum": 1, "maximum": 25, "example": 8, "description": "Cantidad maxima de productos con mas salidas a analizar"},
                      "slowMovementDays": {"type": "integer", "minimum": 1, "maximum": 365, "example": 60, "description": "Dias sin salidas para considerar un producto de poco movimiento"},
                      "slowMovementLimit": {"type": "integer", "minimum": 1, "maximum": 25, "example": 8, "description": "Cantidad maxima de productos con poco movimiento a analizar"},
                      "maxSentences": {"type": "integer", "minimum": 1, "maximum": 10, "example": 3},
                      "language": {"type": "string", "example": "español"}
                    }
                  }
                }
              },
              "paths": {
                "/v1/health": {
                  "get": {"tags": ["Auth"], "security": [], "summary": "Comprobar estado de la API", "responses": {"200": {"$ref": "#/components/responses/Ok"}}}
                },
                "/v1/auth/login": {
                  "post": {
                    "tags": ["Auth"], "security": [], "summary": "Iniciar sesión y obtener JWT",
                    "requestBody": {"required": true, "content": {"application/json": {"schema": {"$ref": "#/components/schemas/LoginRequest"}}}},
                    "responses": {"200": {"$ref": "#/components/responses/Ok"}, "401": {"description": "Credenciales inválidas"}}
                  }
                },
                "/v1/auth/register": {
                  "post": {
                    "tags": ["Auth"], "security": [], "summary": "Registrar un operador",
                    "requestBody": {"required": true, "content": {"application/json": {"schema": {"$ref": "#/components/schemas/RegisterRequest"}}}},
                    "responses": {"201": {"$ref": "#/components/responses/Created"}, "400": {"$ref": "#/components/responses/BadRequest"}}
                  }
                },
                "/v1/users": {
                  "get": {"tags": ["Users"], "summary": "Listar usuarios", "responses": {"200": {"$ref": "#/components/responses/Ok"}}},
                  "post": {
                    "tags": ["Users"], "summary": "Crear usuario",
                    "requestBody": {"required": true, "content": {"application/json": {"schema": {"$ref": "#/components/schemas/UserRequest"}}}},
                    "responses": {"201": {"$ref": "#/components/responses/Created"}, "400": {"$ref": "#/components/responses/BadRequest"}}
                  }
                },
                "/v1/users/{id}": {
                  "get": {"tags": ["Users"], "summary": "Obtener usuario", "parameters": [{"$ref": "#/components/parameters/Id"}], "responses": {"200": {"$ref": "#/components/responses/Ok"}, "404": {"$ref": "#/components/responses/NotFound"}}},
                  "put": {
                    "tags": ["Users"], "summary": "Actualizar usuario", "parameters": [{"$ref": "#/components/parameters/Id"}],
                    "requestBody": {"required": true, "content": {"application/json": {"schema": {"$ref": "#/components/schemas/UserRequest"}}}},
                    "responses": {"200": {"$ref": "#/components/responses/Ok"}, "400": {"$ref": "#/components/responses/BadRequest"}, "404": {"$ref": "#/components/responses/NotFound"}}
                  },
                  "delete": {"tags": ["Users"], "summary": "Desactivar usuario", "parameters": [{"$ref": "#/components/parameters/Id"}], "responses": {"200": {"$ref": "#/components/responses/Ok"}, "404": {"$ref": "#/components/responses/NotFound"}}}
                },
                "/v1/categories": {
                  "get": {"tags": ["Categories"], "summary": "Listar categorías", "responses": {"200": {"$ref": "#/components/responses/Ok"}}},
                  "post": {"tags": ["Categories"], "summary": "Crear categoría", "requestBody": {"required": true, "content": {"application/json": {"schema": {"$ref": "#/components/schemas/CategoryRequest"}}}}, "responses": {"201": {"$ref": "#/components/responses/Created"}}}
                },
                "/v1/categories/{id}": {
                  "get": {"tags": ["Categories"], "summary": "Obtener categoría", "parameters": [{"$ref": "#/components/parameters/Id"}], "responses": {"200": {"$ref": "#/components/responses/Ok"}}},
                  "put": {"tags": ["Categories"], "summary": "Actualizar categoría", "parameters": [{"$ref": "#/components/parameters/Id"}], "requestBody": {"required": true, "content": {"application/json": {"schema": {"$ref": "#/components/schemas/CategoryRequest"}}}}, "responses": {"200": {"$ref": "#/components/responses/Ok"}}},
                  "delete": {"tags": ["Categories"], "summary": "Desactivar categoría", "parameters": [{"$ref": "#/components/parameters/Id"}], "responses": {"200": {"$ref": "#/components/responses/Ok"}}}
                },
                "/v1/suppliers": {
                  "get": {"tags": ["Suppliers"], "summary": "Listar proveedores", "responses": {"200": {"$ref": "#/components/responses/Ok"}}},
                  "post": {"tags": ["Suppliers"], "summary": "Crear proveedor", "requestBody": {"required": true, "content": {"application/json": {"schema": {"$ref": "#/components/schemas/SupplierRequest"}}}}, "responses": {"201": {"$ref": "#/components/responses/Created"}}}
                },
                "/v1/suppliers/{id}": {
                  "get": {"tags": ["Suppliers"], "summary": "Obtener proveedor", "parameters": [{"$ref": "#/components/parameters/Id"}], "responses": {"200": {"$ref": "#/components/responses/Ok"}}},
                  "put": {"tags": ["Suppliers"], "summary": "Actualizar proveedor", "parameters": [{"$ref": "#/components/parameters/Id"}], "requestBody": {"required": true, "content": {"application/json": {"schema": {"$ref": "#/components/schemas/SupplierRequest"}}}}, "responses": {"200": {"$ref": "#/components/responses/Ok"}}},
                  "delete": {"tags": ["Suppliers"], "summary": "Desactivar proveedor", "parameters": [{"$ref": "#/components/parameters/Id"}], "responses": {"200": {"$ref": "#/components/responses/Ok"}}}
                },
                "/v1/products": {
                  "get": {"tags": ["Products"], "summary": "Listar productos", "responses": {"200": {"$ref": "#/components/responses/Ok"}}},
                  "post": {"tags": ["Products"], "summary": "Crear producto", "requestBody": {"required": true, "content": {"application/json": {"schema": {"$ref": "#/components/schemas/ProductRequest"}}}}, "responses": {"201": {"$ref": "#/components/responses/Created"}}}
                },
                "/v1/products/{id}": {
                  "get": {"tags": ["Products"], "summary": "Obtener producto", "parameters": [{"$ref": "#/components/parameters/Id"}], "responses": {"200": {"$ref": "#/components/responses/Ok"}}},
                  "put": {"tags": ["Products"], "summary": "Actualizar producto", "parameters": [{"$ref": "#/components/parameters/Id"}], "requestBody": {"required": true, "content": {"application/json": {"schema": {"$ref": "#/components/schemas/ProductRequest"}}}}, "responses": {"200": {"$ref": "#/components/responses/Ok"}}},
                  "delete": {"tags": ["Products"], "summary": "Desactivar producto", "parameters": [{"$ref": "#/components/parameters/Id"}], "responses": {"200": {"$ref": "#/components/responses/Ok"}}}
                },
                "/v1/inventory": {
                  "get": {"tags": ["Inventory"], "summary": "Consultar inventario actual", "responses": {"200": {"$ref": "#/components/responses/Ok"}}}
                },
                "/v1/inventory-movements": {
                  "get": {"tags": ["Inventory"], "summary": "Listar movimientos", "responses": {"200": {"$ref": "#/components/responses/Ok"}}},
                  "post": {"tags": ["Inventory"], "summary": "Crear movimiento y actualizar stock", "requestBody": {"required": true, "content": {"application/json": {"schema": {"$ref": "#/components/schemas/InventoryMovementRequest"}}}}, "responses": {"201": {"$ref": "#/components/responses/Created"}, "400": {"$ref": "#/components/responses/BadRequest"}}}
                },
                "/v1/inventory-movements/{id}": {
                  "get": {"tags": ["Inventory"], "summary": "Obtener movimiento", "parameters": [{"$ref": "#/components/parameters/Id"}], "responses": {"200": {"$ref": "#/components/responses/Ok"}}},
                  "put": {"tags": ["Inventory"], "summary": "Actualizar movimiento y recalcular stock", "parameters": [{"$ref": "#/components/parameters/Id"}], "requestBody": {"required": true, "content": {"application/json": {"schema": {"$ref": "#/components/schemas/InventoryMovementRequest"}}}}, "responses": {"200": {"$ref": "#/components/responses/Ok"}}},
                  "delete": {"tags": ["Inventory"], "summary": "Eliminar movimiento y revertir stock", "parameters": [{"$ref": "#/components/parameters/Id"}], "responses": {"200": {"$ref": "#/components/responses/Ok"}}}
                },
                "/v1/purchase-requests": {
                  "get": {
                    "tags": ["Purchase Requests"], "summary": "Listar o filtrar solicitudes",
                    "parameters": [{"name": "status", "in": "query", "required": false, "schema": {"type": "string", "enum": ["PENDING", "PURCHASED", "CANCELLED"]}}],
                    "responses": {"200": {"$ref": "#/components/responses/Ok"}}
                  },
                  "post": {"tags": ["Purchase Requests"], "summary": "Crear solicitud", "requestBody": {"required": true, "content": {"application/json": {"schema": {"$ref": "#/components/schemas/PurchaseRequestBody"}}}}, "responses": {"201": {"$ref": "#/components/responses/Created"}}}
                },
                "/v1/purchase-requests/{id}": {
                  "get": {"tags": ["Purchase Requests"], "summary": "Obtener solicitud", "parameters": [{"$ref": "#/components/parameters/Id"}], "responses": {"200": {"$ref": "#/components/responses/Ok"}}},
                  "put": {"tags": ["Purchase Requests"], "summary": "Actualizar solicitud", "parameters": [{"$ref": "#/components/parameters/Id"}], "requestBody": {"required": true, "content": {"application/json": {"schema": {"$ref": "#/components/schemas/PurchaseRequestBody"}}}}, "responses": {"200": {"$ref": "#/components/responses/Ok"}}},
                  "delete": {"tags": ["Purchase Requests"], "summary": "Eliminar solicitud", "parameters": [{"$ref": "#/components/parameters/Id"}], "responses": {"200": {"$ref": "#/components/responses/Ok"}}}
                },
                "/v1/purchase-requests/{id}/status": {
                  "patch": {
                    "tags": ["Purchase Requests"], "summary": "Actualizar estado de solicitud", "parameters": [{"$ref": "#/components/parameters/Id"}],
                    "requestBody": {"required": true, "content": {"application/json": {"schema": {"$ref": "#/components/schemas/PurchaseStatusRequest"}}}},
                    "responses": {"200": {"$ref": "#/components/responses/Ok"}}
                  }
                },
                "/v1/imports": {
                  "get": {"tags": ["Imports"], "summary": "Listar importaciones", "responses": {"200": {"$ref": "#/components/responses/Ok"}}}
                },
                "/v1/imports/{id}": {
                  "get": {"tags": ["Imports"], "summary": "Obtener importación con items", "parameters": [{"$ref": "#/components/parameters/Id"}], "responses": {"200": {"$ref": "#/components/responses/Ok"}}}
                },
                "/v1/imports/preview": {
                  "post": {
                    "tags": ["Imports"], "summary": "Crear preview de importación",
                    "requestBody": {"required": true, "content": {"application/json": {"schema": {"$ref": "#/components/schemas/ImportPreviewRequest"}}}},
                    "responses": {"201": {"$ref": "#/components/responses/Created"}}
                  }
                },
                "/v1/imports/{id}/confirm": {
                  "post": {"tags": ["Imports"], "summary": "Confirmar importación y crear movimientos VALID", "parameters": [{"$ref": "#/components/parameters/Id"}], "responses": {"200": {"$ref": "#/components/responses/Ok"}}}
                },
                "/v1/imports/{id}/cancel": {
                  "post": {"tags": ["Imports"], "summary": "Cancelar importación", "parameters": [{"$ref": "#/components/parameters/Id"}], "responses": {"200": {"$ref": "#/components/responses/Ok"}}}
                },
                "/v1/ai/summaries": {
                  "post": {
                    "tags": ["AI"], "summary": "Generar alertas y recomendaciones de inventario con OpenAI",
                    "requestBody": {"required": false, "content": {"application/json": {"schema": {"$ref": "#/components/schemas/SummaryRequest"}}}},
                    "responses": {"200": {"$ref": "#/components/responses/Ok"}, "400": {"$ref": "#/components/responses/BadRequest"}, "401": {"$ref": "#/components/responses/Unauthorized"}, "503": {"$ref": "#/components/responses/ServiceUnavailable"}}
                  }
                }
              }
            }
            """;
        return Response.ok(json).build();
    }
}
