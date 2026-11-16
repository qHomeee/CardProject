"""Ответы с ошибками каталога."""

from fastapi import Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from sqlalchemy.exc import DBAPIError, TimeoutError


class CatalogError(Exception):
    """Ошибка, предусмотренная контрактом каталога."""

    def __init__(self, status: int, code: str, message: str):
        super().__init__(message)
        self.status = status
        self.code = code


def error_response(
    status: int, code: str, message: str, fields: list[dict[str, str]] | None = None,
) -> JSONResponse:
    """Формирует ответ без внутренних параметров и трассировки."""
    body = {"code": code, "message": message}
    if fields is not None:
        body["fields"] = fields
    headers = {"WWW-Authenticate": "APIKey"} if status == 401 else None
    return JSONResponse(status_code=status, content={"error": body}, headers=headers)


async def catalog_error_handler(_request: Request, error: CatalogError) -> JSONResponse:
    """Возвращает ошибку каталога."""
    return error_response(error.status, error.code, str(error))


async def validation_error_handler(_request: Request, error: RequestValidationError) -> JSONResponse:
    """Показывает путь к полю без отклонённого значения."""
    fields = [
        {"field": ".".join(map(str, item["loc"])), "message": "Проверьте значение поля."}
        for item in error.errors()
    ]
    return error_response(422, "validation_error", "Проверьте поля запроса.", fields)


async def database_error_handler(_request: Request, error: DBAPIError | TimeoutError) -> JSONResponse:
    """Отличает временную недоступность БД от нарушения ожидаемого контракта."""
    if isinstance(error, TimeoutError):
        unavailable = True
    else:
        code = getattr(error.orig, "sqlstate", None)
        unavailable = error.connection_invalidated or code is None or code.startswith("08") or code in {
            "57P01", "57P02", "57P03",
        }
    if unavailable:
        return error_response(503, "service_unavailable", "Операция временно недоступна.")
    return error_response(500, "internal_error", "Не удалось выполнить запрос.")


async def internal_error_handler(_request: Request, _error: Exception) -> JSONResponse:
    """Возвращает общий ответ при непредвиденном сбое."""
    return error_response(500, "internal_error", "Не удалось выполнить запрос.")
