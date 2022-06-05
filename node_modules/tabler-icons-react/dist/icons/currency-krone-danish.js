import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function CurrencyKroneDanish(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-currency-krone-danish",
    width: size,
    height: size,
    viewBox: "0 0 24 24",
    stroke: color,
    strokeWidth: "2",
    fill: "none",
    strokeLinecap: "round",
    strokeLinejoin: "round"
  }, restProps), /*#__PURE__*/React.createElement("path", {
    stroke: "none",
    d: "M0 0h24v24H0z",
    fill: "none"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M5 6v12"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M5 12c3.5 0 6 -3 6 -6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M5 12c3.5 0 6 3 6 6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M15 10v8"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M19 10a4 4 0 0 0 -4 4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M20 18.01v-.01"
  }));
}

export { CurrencyKroneDanish as default };
