import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Forms(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-forms",
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
    d: "M12 3a3 3 0 0 0 -3 3v12a3 3 0 0 0 3 3"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M6 3a3 3 0 0 1 3 3v12a3 3 0 0 1 -3 3"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M13 7h7a1 1 0 0 1 1 1v8a1 1 0 0 1 -1 1h-7"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M5 7h-1a1 1 0 0 0 -1 1v8a1 1 0 0 0 1 1h1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M17 12h.01"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M13 12h.01"
  }));
}

export { Forms as default };
