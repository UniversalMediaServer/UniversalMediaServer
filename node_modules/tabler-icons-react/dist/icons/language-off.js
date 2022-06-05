import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function LanguageOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-language-off",
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
    d: "M4 5h1m4 0h2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M9 3v2m-.508 3.517c-.814 2.655 -2.52 4.483 -4.492 4.483"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M5 9c-.003 2.144 2.952 3.908 6.7 4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 20l2.463 -5.541m1.228 -2.764l.309 -.695l.8 1.8"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M18 18h-5.1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { LanguageOff as default };
