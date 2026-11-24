"""Ответы с ошибками каталога."""

from fastapi import Request
from fastapi.exceptions import RequestValidationError
from fastapi.exception_handlers import http_exception_handler
from fastapi.responses import JSONResponse, Response
from sqlalchemy.exc import DBAPIError, OperationalError, TimeoutError
from starlette.exceptions import HTTPException

HTTP_ERRORS = {
    404: ("route_not_found", "Маршрут API не найден."),
    405: ("method_not_allowed", "Метод для этого маршрута недоступен."),
}
FIELD_MESSAGES = {
    "missing": "Укажите значение поля.",
    "string_type": "Передайте строку.",
    "string_too_short": "Введите значение.",
    "extra_forbidden": "Поле не предусмотрено контрактом.",
    "uuid_parsing": "Укажите корректный UUID.",
    "json_invalid": "Исправьте синтаксис JSON.",
}


class CatalogError(Exception):
    """Ошибка, предусмотренная контрактом каталога."""

    def __init__(self, status: int, code: str, message: str):
        super().__init__(message)
        self.status = status
        self.code = code


def error_response(
    status: int,
    code: str,
    message: str,
    fields: list[dict[str, str]] | None = None,
    *,
    headers: dict[str, str] | None = None,
) -> JSONResponse:
    """Формирует ответ без внутренних параметров и трассировки."""
    body = {"code": code, "message": message}
    if fields is not None:
        body["fields"] = fields
    if status == 401:
        headers = {"WWW-Authenticate": "APIKey", **(headers or {})}
    return JSONResponse(status_code=status, content={"error": body}, headers=headers)


async def catalog_error_handler(_request: Request, error: CatalogError) -> JSONResponse:
    """Возвращает ошибку каталога."""
    return error_response(error.status, error.code, str(error))


async def catalog_http_error_handler(request: Request, error: HTTPException) -> Response:
    """Сохраняет формат ошибок каталога для неизвестного маршрута и метода."""
    if request.url.path.startswith("/api/v1/") and error.status_code in HTTP_ERRORS:
        code, message = HTTP_ERRORS[error.status_code]
        return error_response(error.status_code, code, message, headers=error.headers)
    return await http_exception_handler(request, error)


async def validation_error_handler(_request: Request, error: RequestValidationError) -> JSONResponse:
    """Показывает путь к полю и безопасное пояснение."""
    fields = []
    for item in error.errors():
        kind = item["type"]
        location = item["loc"][:1] if kind == "json_invalid" else item["loc"]
        message = FIELD_MESSAGES.get(kind, "Проверьте значение поля.")
        if kind == "string_too_long":
            maximum = item["ctx"]["max_length"]
            message = f"Допустимо не более {maximum} символов."
        fields.append({"field": ".".join(map(str, location)), "message": message})
    return error_response(422, "validation_error", "Проверьте поля запроса.", fields)


async def database_error_handler(_request: Request, error: DBAPIError | TimeoutError) -> JSONResponse:
    """Отличает временную недоступность БД от нарушения ожидаемого контракта."""
    if isinstance(error, TimeoutError):
        unavailable = True
    else:
        code = getattr(error.orig, "sqlstate", None)
        unavailable = (
            error.connection_invalidated
            or (code is None and isinstance(error, OperationalError))
            or (code is not None and (code.startswith("08") or code in {"57P01", "57P02", "57P03"}))
        )
    if unavailable:
        return error_response(503, "service_unavailable", "Операция временно недоступна.")
    return error_response(500, "internal_error", "Не удалось выполнить запрос.")


async def internal_error_handler(_request: Request, _error: Exception) -> JSONResponse:
    """Возвращает общий ответ при непредвиденном сбое."""
    return error_response(500, "internal_error", "Не удалось выполнить запрос.")
